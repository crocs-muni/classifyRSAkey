#!/usr/bin/env python3

import json
import os
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import matplotlib.patches as mpatches
import matplotlib.lines as mlines
import sys
import math
from re import findall
import datetime
import csv
import numpy as np
from matplotlib.ticker import MultipleLocator, FormatStrFormatter, LogLocator, \
    FuncFormatter, LogFormatter

matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42

if len(sys.argv) < 4:
    print("Usage: {0} classification_table.csv Name_of_domain classification_dir(s)...".format(
        sys.argv[0]))
    print("  To obtain the classification table in csv format from json,\n"
          "    use the \"-ec in_table.json out_table.csv\" option of the classification tool")
    print("  Name_of_domain (e.g. PGP, TLS) will be used in graph titles")
    print("  classification_dir(s)... are directories with outputs "
          "of classification, containing the file \"prior_probability.json\"; "
          "the directories must contain a date or a timestamp in the name")
    exit(1)

table_name = sys.argv[1]
source = sys.argv[2]
directories = sys.argv[3:]

with open(table_name, "r") as f:
    table_lines = f.readlines()

valid_table_lines = []
found_start = False
for row in table_lines:
    if found_start:
        valid_table_lines.append(row.replace("-", "0"))
    else:
        if row.startswith("Bits"):
            valid_table_lines.append(row)
            found_start = True

table_reader = csv.reader(valid_table_lines, delimiter=",")
masks = []
group_mask_count = dict()

header = None

for row in table_reader:
    if row[0] == 'Bits':
        header = row
        for i in range(1, len(row)):
            group_mask_count[header[i]] = dict()
        continue
    mask = row[0]
    masks.append(mask)
    for i in range(1, len(row)):
        group_mask_count[header[i]][mask] = float(row[i])

filename = "prior_probability.json"


def numbers_in_text(text):
    return [int(s) for s in findall(r'\d+', os.path.basename(text))]


def time_from_path(directory_path):
    leaf_dir = os.path.splitext(os.path.basename(directory_path))[0]
    numbers = numbers_in_text(leaf_dir)
    if 3 <= len(numbers) < 5:
        # possibly the YYYY-MM-DD format
        time = datetime.date(numbers[0], numbers[1], numbers[2])
        return time
    timestamp = None
    if leaf_dir.find("fullmerge_certs") != -1:
        time = datetime.date(numbers[0], numbers[1], 28)
        return time
    if len(numbers) >= 5:
        timestamp = numbers[2]
    if len(numbers) >= 1:
        if timestamp is None:
            timestamp = numbers[0]
        if 19700000 <= timestamp <= 20380119:
            # YYYYMMDD without separators
            y = timestamp // 10000
            m = (timestamp - y * 10000) // 100
            d = (timestamp - y * 10000 - m * 100)
            time = datetime.date(y, m, d)
            return time
        while timestamp > 2 ** 31:
            timestamp /= 1000  # possibly milli or micro seconds
        time = datetime.date.fromtimestamp(timestamp)
        return time
    return 0


def dataset_name(directory_path):
    return os.path.splitext(os.path.basename(directory_path))[0][0:13]


name_to_frequencies = dict()
name_to_probabilities = dict()
extra_name_to_frequencies = dict()
extra_name_to_probabilities = dict()
groups = set()
datasets = list()
extra_datasets = list()
group_sources = dict()
max_prob = 0

data_sources_to_start = dict()
data_sources_to_end = dict()

dates = []

delta_extra = datetime.timedelta(days=32)

for directory in directories:
    file = os.path.join(directory, filename)
    if not os.path.isfile(file):
        print("File does not exist: {0}".format(file))
        continue
    f = open(file, "r")
    prior = json.load(f)
    f.close()
    name = time_from_path(directory)

    data_source = os.path.basename(os.path.dirname(directory))
    if data_source not in data_sources_to_start:
        data_sources_to_start[data_source] = name
        data_sources_to_end[data_source] = name
    else:
        if data_sources_to_start[data_source] > name:
            data_sources_to_start[data_source] = name
        if data_sources_to_end[data_source] < name:
            data_sources_to_end[data_source] = name

    frequencies = prior["frequencies"]
    probabilities = prior["probability"]

    is_extra = name in name_to_frequencies
    if not is_extra:
        has_preceding = False
        has_following = False
        for d in dates:
            if name - delta_extra < d < name:
                has_preceding = True
            if name + delta_extra > d > name:
                has_following = True
            if has_preceding and has_following:
                is_extra = True
                break

    if is_extra:  # name in name_to_frequencies:
        extra_name_to_frequencies[name] = frequencies
        extra_name_to_probabilities[name] = probabilities
        extra_datasets.append(name)
    else:
        name_to_frequencies[name] = frequencies
        name_to_probabilities[name] = probabilities
        datasets.append(name)
    dates.append(name)
    groups = groups.union(probabilities.keys())
    group_sources = prior["groups"]

datasets.sort()
groups = list(groups)
groups.sort()
masks = list(masks)
masks.sort()

group_probabilities = [[name_to_probabilities[name][x] for x in groups] for name
                       in datasets]

max_keys = 0

name_to_key_count = dict()
for dataset in datasets:
    key_count = 0
    for count in name_to_frequencies[dataset].values():
        key_count += int(count)
    name_to_key_count[dataset] = key_count

extra_name_to_key_count = dict()
for dataset in extra_datasets:
    key_count = 0
    for count in extra_name_to_frequencies[dataset].values():
        key_count += int(count)
    extra_name_to_key_count[dataset] = key_count
    if key_count > max_keys:
        max_keys = key_count

line_styles = ["solid", "dashed", "dashdot", "dotted"]
markers = ('o', 'v', '^', '<', '>', '8', 's', 'p', '*', 'h', 'H', 'D', 'd', 'P', 'X')

chars_per_line = 50

colors = dict()

if len(groups) == 13:
    cs = plt.cm.Vega20(np.linspace(0, 1, 20))
    color_sequence = [cs[1], cs[3], cs[5], cs[7], cs[9], cs[11],
                      cs[4], cs[17], cs[12], cs[15], cs[18], cs[6], cs[2]]
elif len(groups) == 12:
    cs = plt.cm.Vega20(np.linspace(0, 1, 20))
    color_sequence = [cs[1], cs[3], cs[5], cs[7], cs[9], cs[11],
                      cs[4], cs[12], cs[15], cs[18], cs[6], cs[2]]
else:
    color_sequence = plt.cm.Vega20(np.linspace(0, 1, len(groups)))

if source.lower().find("pgp") != -1 or source.lower().find("maven") != -1:
    bold_sources = ["OpenSSL", "PGPSDK", "YubiKey", "Libgcrypt",
                    "Bouncy Castle"]
elif source.lower().find("github") != -1:
    bold_sources = ["OpenSSL", "PuTTY"]
else:
    bold_sources = ["OpenSSL", "Bouncy Castle",  # "Crypto++",
                    "Libgcrypt", "Microsoft", "Nettle", "OpenJDK",
                    "mbedTLS", "Botan", "LibTomCrypt", "WolfSSL", "cryptlib"]

for c in color_sequence:
    print("\"#%0.2X%0.2X%0.2X\"," % (int(c[0]*255), int(c[1]*255), int(c[2]*255)))

for group in groups:
    sources = group_sources[group]
    # adjusting the legend
    sources = [s.replace("& J3A081", "\n                & J3A081") for s in sources]
    sources = [s.replace("& 1.0.2k", "\n                & 1.0.2k") for s in sources]
    for bold in bold_sources:
        sources = [s.replace(bold, r"$\mathbf{{{0:}}}$".format(bold)) for s in sources]
    sources = [s.replace("Crypto++", r"$\mathbf{{Crypto}}$++") for s in sources]

    group_sources[group] = sources

source_filename = source.replace("/", "_")

half_size = False

for absolute in [True, False]:
    if half_size:
        fig = plt.figure(figsize=(7, 3.8))
        plt.subplots_adjust(left=0.1, bottom=0.27, right=0.98, top=0.99,
                            wspace=0.01, hspace=0.01)
    else:
        fig = plt.figure(figsize=(10.2, 7))
        left = 0.055
        if absolute:
            left = 0.07
        plt.subplots_adjust(left=left, bottom=0.42, right=0.99, top=0.99,
                        wspace=0.01, hspace=0.01)

    years = mdates.YearLocator()  # every year
    months = mdates.MonthLocator()  # every month
    quartals = mdates.MonthLocator(interval=3)
    days = mdates.DayLocator()
    yearsFmt = mdates.DateFormatter('%Y')
    monthsFmt = mdates.DateFormatter('%m/%y')

    delta = datetime.timedelta(days=7)

    ax = plt.axes()
    if half_size:
        ax.xaxis.set_major_locator(months)
    else:
        ax.xaxis.set_major_locator(quartals)
    ax.xaxis.set_major_formatter(monthsFmt)
    ax.xaxis.set_minor_locator(months)

    plt.gca().set_xlim(left=min(datasets) - delta, right=max(datasets) + delta)

    extra_data_exists = len(extra_name_to_probabilities) != 0
    if extra_data_exists:
        extra_samples = [False, True]
    else:
        extra_samples = [False]

    for extra in extra_samples:
        i = 0
        plotted = dict()
        for group in groups:
            sources = group_sources[group]
            sources_string = ""
            chars = 0
            for j in range(0, len(sources)):
                sources_string += sources[j]
                chars += len(sources[j])
                if j != len(sources) - 1:
                    sources_string += ", "
                    if chars > chars_per_line:
                        sources_string += "\n                 "
                        chars = 0
            if absolute:
                if extra:
                    data = [float(extra_name_to_probabilities[name][group])
                            * extra_name_to_key_count[name] for name in extra_datasets]
                else:
                    data = [float(name_to_probabilities[name][group])
                            * name_to_key_count[name] for name in datasets]
                plt.ylabel("Estimated number of keys")
                if max_keys > 5000000:
                    to_absolute_formatter = FuncFormatter(
                        lambda x, pos: "{0:1.0f}M".format(float(x / 1000000)))
                    ax.yaxis.set_major_locator(MultipleLocator(1000000))
                    ax.yaxis.set_major_formatter(to_absolute_formatter)
                else:
                    to_absolute_formatter = FuncFormatter(
                        lambda x, pos: "{0:1.1f}M".format(float(x / 1000000)))
                    ax.yaxis.set_major_formatter(to_absolute_formatter)

            else:
                if extra:
                    data = [float(extra_name_to_probabilities[name][group])
                            for name in extra_datasets]
                else:
                    data = [float(name_to_probabilities[name][group])
                            for name in datasets]
                plt.ylabel("Estimated share of a given source")
            alpha = 0.75
            xaxis = datasets
            color = color_sequence[i % len(color_sequence)]
            if extra:
                alpha /= 3
                xaxis = extra_datasets
            markersize = 4
            if half_size:
                markersize = 3
            data, = plt.plot(xaxis, data,
                            label="{0}: {1}".format(group, sources_string),
                            marker=markers[i % len(markers)],
                            linestyle=line_styles[i % len(line_styles)],
                            color=color,
                            linewidth=1, alpha=alpha, markersize=markersize)
            colors[group] = data.get_color()
            plotted[group] = data
            i += 1

        if not extra:
            # dummy stuff to balance the legend
            plt.plot(0, 0, color="white", alpha=0, label=" ")
            plt.plot(0, 0, color="white", alpha=0, label=" ")
            if len(groups) >= 13:
                plt.plot(0, 0, color="white", alpha=0, label=" ")

            if half_size:
                for x in groups:
                    plotted[x].set_label(x.replace("Group ", "").replace(" ", ""))
                handles = [plotted[x] for x in groups]
                legend = plt.legend(handles=handles, loc='upper center',
                                    bbox_to_anchor=(0.48, -0.165), ncol=7)
            else:
                legend = plt.legend(loc='upper center', bbox_to_anchor=(0.48, -0.1),
                           ncol=2, fancybox=False, shadow=False, handlelength=3,
                           frameon=False)
    plt.xticks(rotation=45)

    beautify_name = {"eff": "EFF SSL Observatory",
                     "full_eco": "HTTPS Ecosystem",
                     "full_sonarssl": "Rapid7 Sonar",
                     "full_tls": "Censys IPv4 TLS scan",
                     "eco": "HTTPS Ecosystem",
                     "sonar": "Rapid7 Sonar",
                     "tls": "Censys IPv4 TLS scan"}

    if len(data_sources_to_start.keys()) > 1:
        data_source_index = 0
        for data_source in sorted(data_sources_to_start.keys()):
            data_source_index += 1
            start = data_sources_to_start[data_source]
            stop = data_sources_to_end[data_source]
            print(data_source, start, stop)
            plt.axvline(start, color="g", alpha=0.5)
            plt.axvline(stop, color="r", alpha=0.5)
            height = (0.23 + data_source_index*0.05)*max_keys
            color = "black"
            plt.plot([start, stop], [height, height], color=color)
            plt.text(start + delta, height + 0.01*max_keys,
                     beautify_name[data_source] if data_source in beautify_name else data_source)

    if absolute and source_filename == "all_samples":
        note_color = "gray"
        date_fix = datetime.date(2015, 6, 30)
        date_fail = datetime.date(2015, 12, 30)
        ax.arrow(date_fix, 3000000, 0, -200000,
                 head_width=20, head_length=200000, color=note_color)
        plt.text(date_fix - datetime.timedelta(days=8.5*30), 3100000,
                 "Sonar fixed TLS 1.2 handshake",
                 color=note_color).set_fontsize(10)

        ax.arrow(date_fail, 5900000, 0, 200000,
                 head_width=20, head_length=200000, color=note_color)
        plt.text(date_fail - datetime.timedelta(days=4.5*30), 5600000,
                 "Unfinished Sonar scan",
                 color=note_color).set_fontsize(10)

    if absolute:
        fig.savefig("{0}_absoluteintime.pdf".format(source_filename))
    else:
        fig.savefig("{0}_percentageintime.pdf".format(source_filename))

count = len(datasets)
rows = math.ceil(math.sqrt(count))
if rows < 1:
    rows = 1
cols = math.ceil(count / rows)

def labeling(bars):
    for bar in bars:
        height = bar.get_height()
        plt.text(bar.get_x() + bar.get_width() / 2.0, height + 0.01,
                 '%0.1f' % (int(1000*height)/10.0),
                 ha='center', va='bottom')


for plot_type in ["distribution", "barplot"]:
    if plot_type == "distribution":
        fig = plt.figure(figsize=(5 * rows, 4 * cols))
    else:
        fig = plt.figure(figsize=(4.2 * rows, 2.7 * cols))
    i = 0
    for dataset in datasets:
        i += 1
        sub = fig.add_subplot(rows, cols, i)
        frequencies = name_to_frequencies[dataset]
        size = sum(frequencies.values())
        if len(datasets) < 9:
            if plot_type == "distribution":
                plt.subplots_adjust(left=0.10, bottom=0.08, right=0.99, top=0.9,
                                    wspace=0.2, hspace=0.5)
            else:
                plt.subplots_adjust(left=0.14, bottom=0.18, right=0.99, top=0.9,
                                    wspace=0.2, hspace=0.5)
        else:
            plt.subplots_adjust(left=0.05, bottom=0.05, right=0.95, top=0.95,
                                wspace=0.2, hspace=0.5)
        plt.xticks([0, 64, 128, 192, 255], [0, 64, 128, 192, 255])
        if len(datasets) == 1:
            plt.title(source.replace("_", " "))
        else:
            plt.title(dataset)
        if plot_type == "distribution":
            plt.bar(range(0, len(masks)),
                    [100 * (frequencies[mask] / size)
                     if mask in frequencies
                     else 0 for mask in masks])
        else:
            labels = list(name_to_probabilities[dataset].keys())
            labels.sort()
            group_numbers = [int(x.split(" ")[-1]) for x in labels]
            fracs = [float(name_to_probabilities[dataset][x]) for x in labels]
            r = plt.bar(group_numbers, fracs,
                        color=[colors[g] for g in sorted(group_mask_count.keys())])
            plt.xticks(group_numbers, group_numbers)
            plt.axis([min(group_numbers) - 0.75, max(group_numbers) + 0.75, 0, 1.1])
            yvals = range(0, 125, 25)
            plt.yticks([x/100 for x in yvals], yvals)
            plt.xlabel("Group number")
            plt.ylabel("Prior probability [%]")
            labeling(r)

    if plot_type == "distribution":
        fig.savefig("{0}_distributions.pdf".format(source_filename))
    else:
        fig.savefig("{0}_classification.pdf".format(source_filename))

count = len(group_mask_count.keys())
rows = math.ceil(math.sqrt(count))
if rows < 1:
    rows = 1
cols = math.ceil(count / rows)
if count == 13:
    cols = 5
    rows = 3

fig = plt.figure(figsize=(3 * cols, 1.5 * rows))
i = 0
for group in sorted(group_mask_count.keys()):
    i += 1
    sub = fig.add_subplot(rows, cols, i)
    mask_count = group_mask_count[group]
    frequencies = [mask_count[m] if m in mask_count else 0 for m in masks]
    size = sum(frequencies)
    plt.subplots_adjust(left=0.05, bottom=0.05, right=0.95, top=0.95,
                        wspace=0.2, hspace=0.5)
    plt.title(group)
    plt.bar(range(0, len(masks)),
            [100 * (f / size) for f in frequencies],
            color=colors[group])
    plt.xticks([0, 64, 128, 192, 255], [0, 64, 128, 192, 255])
fig.savefig("{0}_profiles.pdf".format(source_filename))

prior = name_to_probabilities[datasets[-1]]
distribution = name_to_frequencies[datasets[-1]]

fig = plt.figure(figsize=(5, 3))
plt.subplots_adjust(left=0.13, bottom=0.14, right=0.99, top=0.91,
                                wspace=0.2, hspace=0.5)
bottoms = [0 for x in masks]
groups_and_probs = []
keys_total = sum(distribution.values())
for group in sorted(group_mask_count.keys()):
    mask_count = group_mask_count[group]
    frequencies = [mask_count[m] if m in mask_count else 0 for m in masks]
    size = sum(frequencies)
    frequencies = [100 * float(prior[group]) * (mask_count[m] / size) if m in mask_count else 0 for m in masks]
    print(prior[group])
    groups_and_probs.append((group, float(prior[group])))
    plt.bar(range(0, len(masks)),
            frequencies,
            bottom=bottoms,
            color=colors[group])
    bottoms = [bottoms[i] + frequencies[i] for i in range(len(frequencies))]
plt.xticks([0, 64, 128, 192, 255], [0, 64, 128, 192, 255])
frequencies = [distribution[m] if m in distribution else 0 for m in masks]
size = sum(frequencies)
frequencies = [100 * (f / size) for f in frequencies]
original_line, = plt.plot(range(0, len(masks)), frequencies, color="black",
                          alpha=0.7, label="Original distribution",
                          linestyle="--", linewidth=1.5)
plt.title(source.replace("_", " "), fontsize=16)
groups_and_probs.sort(key=lambda x: x[1], reverse=True)
groups_and_probs = [x for x in groups_and_probs if x[1] >= 0.001]
patches = [mpatches.Patch(color=colors[x[0]],
                          label="{0}: {1:3.2f}%"
                          .format(x[0].replace("  ", "   "), x[1]*100))
           for x in groups_and_probs]
patches.insert(0, original_line)
keys_total_label = "Total keys: {0:,}".format(keys_total)
plt.legend(handles=patches, title=keys_total_label,
           loc='upper right')
plt.xlabel("Mask")
plt.ylabel("Number of keys with a given mask")
ax = plt.axes()
max_keys = max(distribution.values())

to_absolute_formatter = FuncFormatter(
    lambda x, pos: "{0}K".format(int(round(keys_total*x/100/1000))))
ax.yaxis.set_major_formatter(to_absolute_formatter)

if max_keys < 10000:
    ax.yaxis.set_major_locator(MultipleLocator(100000 / keys_total))
    ax.yaxis.set_minor_locator(MultipleLocator(10000 / keys_total))
elif max_keys < 50000:
    ax.yaxis.set_major_locator(MultipleLocator(500000 / keys_total))
    ax.yaxis.set_minor_locator(MultipleLocator(100000 / keys_total))
elif max_keys < 100000:
    ax.yaxis.set_major_locator(MultipleLocator(1000000 / keys_total))
    ax.yaxis.set_minor_locator(MultipleLocator(100000 / keys_total))
elif max_keys < 1000000:
    ax.yaxis.set_major_locator(MultipleLocator(5000000 / keys_total))
    ax.yaxis.set_minor_locator(MultipleLocator(1000000 / keys_total))
else:
    ax.yaxis.set_major_locator(MultipleLocator(10000000 / keys_total))
    ax.yaxis.set_minor_locator(MultipleLocator(5000000 / keys_total))
    to_absolute_formatter = FuncFormatter(
    lambda x, pos: "{0:1.1f}M".format(keys_total*x/100/1000000))
    ax.yaxis.set_major_formatter(to_absolute_formatter)


fig.savefig("{0}_stacked.pdf".format(source_filename))
