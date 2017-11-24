#!/usr/bin/env python3

import json
import os
from fnmatch import fnmatch
import matplotlib
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import matplotlib.patches as mpatches
from re import findall
import datetime
import csv
import numpy as np
from matplotlib.ticker import MultipleLocator, FuncFormatter
from enum import Enum
import argparse


class Table:
    def __init__(self):
        pass

    @staticmethod
    def from_csv(table_name):
        with open(table_name, "r") as f:
            table_lines = f.readlines()

        group_sources = dict()

        valid_table_lines = []
        found_start = False
        for row in table_lines:
            if found_start:
                valid_table_lines.append(row.replace("-", "0"))
            else:
                if row.startswith("Bits"):
                    valid_table_lines.append(row)
                    found_start = True
                elif not row.startswith("Group name,Group sources") and row.strip() != "":
                    row_split = row.strip().split(",")
                    group_sources[row_split[0]] = row_split[1:]

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
        table = Table()
        table.masks = masks
        table.group_mask_count = group_mask_count
        table.group_sources = group_sources
        table.groups = sorted(group_sources.keys())
        return table


class Classification:
    def __init__(self):
        pass

    @staticmethod
    def from_json(file_path):
        f = open(file_path, "r")
        prior = json.load(f)
        f.close()

        frequencies = prior["frequencies"]
        probabilities = prior["probability"]
        group_sources = prior["groups"]

        classification = Classification()
        classification.frequencies = frequencies
        classification.probabilities = probabilities
        classification.group_sources = group_sources
        classification.key_count = sum(frequencies.values())
        return classification


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


class DatasetHistory:
    def __init__(self):
        pass

    filename = "prior_probability.json"

    @staticmethod
    def from_directory(directory):
        data_source = os.path.basename(os.path.dirname(directory))
        classifications = []

        directories = []
        for path, subdirs, files in os.walk(directory):
            for name in files:
                if fnmatch(name, DatasetHistory.filename):
                    directories.append(path)

        for d in directories:
            timestamp = time_from_path(d)
            file_path = os.path.join(d, DatasetHistory.filename)
            if not os.path.isfile(file_path):
                print("File does not exist: {0}".format(file_path))
                continue
            classification = Classification.from_json(file_path)
            classification.timestamp = timestamp
            classifications.append(classification)

        classifications.sort(key=lambda x: x.timestamp)

        history = DatasetHistory()
        history.source = data_source
        history.classifications = classifications

        return history


def draw_history(source, table, histories, colors, half_size=False, absolute=True):
    line_styles = ["solid", "dashed", "dashdot", "dotted"]
    markers = ('o', 'v', '^', '<', '>', '8', 's', 'p', '*', 'h', 'H', 'D', 'd', 'P', 'X')

    chars_per_line = 50

    if source.lower().find("pgp") != -1 or source.lower().find("maven") != -1:
        bold_sources = ["OpenSSL", "PGPSDK", "YubiKey", "Libgcrypt",
                        "Bouncy Castle"]
    elif source.lower().find("github") != -1:
        bold_sources = ["OpenSSL", "PuTTY"]
    else:
        bold_sources = ["OpenSSL", "Bouncy Castle",  # "Crypto++",
                        "Libgcrypt", "Microsoft", "Nettle", "OpenJDK",
                        "mbedTLS", "Botan", "LibTomCrypt", "WolfSSL", "cryptlib"]

    group_sources = dict()
    for group, sources in table.group_sources.items():
        sources = [s.replace("& J3A081", "\n                & J3A081") for s in sources]
        sources = [s.replace("& 1.0.2k", "\n                & 1.0.2k") for s in sources]
        for bold in bold_sources:
            sources = [s.replace(bold, r"$\mathbf{{{0:}}}$".format(bold)) for s in sources]
        sources = [s.replace("Crypto++", r"$\mathbf{{Crypto}}$++") for s in sources]

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

        group_sources[group] = sources_string

    beautify_name = {"eff": "EFF SSL Observatory",
                     "full_eco": "HTTPS Ecosystem",
                     "full_sonarssl": "Rapid7 Sonar",
                     "full_tls": "Censys IPv4 TLS scan",
                     "eco": "HTTPS Ecosystem",
                     "sonar": "Rapid7 Sonar",
                     "tls": "Censys IPv4 TLS scan"}

    source_filename = source.replace("/", "_")

    if half_size:
        fig = plt.figure(figsize=(7, 3.8))
        plt.subplots_adjust(left=0.1, bottom=0.27, right=0.98, top=0.99,
                            wspace=0.01, hspace=0.01)
        markersize = 3
    else:
        fig = plt.figure(figsize=(10.2, 7))
        left = 0.055
        if absolute:
            left = 0.07
        plt.subplots_adjust(left=left, bottom=0.42, right=0.99, top=0.99,
                            wspace=0.01, hspace=0.01)
        markersize = 4

    years = mdates.YearLocator()  # every year
    months = mdates.MonthLocator()  # every month
    quartals = mdates.MonthLocator(interval=3)
    days = mdates.DayLocator()
    yearsFmt = mdates.DateFormatter('%Y')
    monthsFmt = mdates.DateFormatter('%m/%y')

    delta = datetime.timedelta(days=7)
    if half_size:
        delta = datetime.timedelta(days=14)

    ax = plt.axes()

    dates = [c.timestamp for h in histories for c in h.classifications]

    plt.gca().set_xlim(left=min(dates) - delta, right=max(dates) + delta)
    plt.xticks(rotation=45)

    ax.xaxis.set_major_locator(quartals)
    if half_size and max(dates) - min(dates) < datetime.timedelta(days=18*30):
        ax.xaxis.set_major_locator(quartals)

    ax.xaxis.set_major_formatter(monthsFmt)
    ax.xaxis.set_minor_locator(months)

    max_keys = max([c.key_count for h in histories for c in h.classifications])
    if absolute:
        if max_keys > 5000000:
            to_absolute_formatter = FuncFormatter(
                lambda x, pos: "{0:1.0f}M".format(float(x / 1000000)))
            ax.yaxis.set_major_locator(MultipleLocator(1000000))
            ax.yaxis.set_major_formatter(to_absolute_formatter)
            plt.ylabel("Estimated number of keys")
        else:
            to_absolute_formatter = FuncFormatter(
                lambda x, pos: "{0:1.1f}M".format(float(x / 1000000)))
            ax.yaxis.set_major_formatter(to_absolute_formatter)
            plt.ylabel("Estimated share of a given source")

    covered_dates = []
    groups = table.groups
    plotted = dict()
    for history in histories:
        dates = [c.timestamp for c in history.classifications]
        dates.sort()
        covered = [False for _ in dates]

        for cd in covered_dates:
            for d in range(0, len(dates)):
                if cd[0] <= dates[d] <= cd[1]:
                    covered[d] = True

        segments = []
        opened = False
        last = False
        start = 0
        for d in range(0, len(dates)):
            if opened and covered[d] != last or d == len(dates) - 1:
                segments.append((start, d + 1))
                opened = False
            if not opened:
                start = d
                last = covered[d]
                opened = True

        i = 0
        for group in groups:
            sources_string = group_sources[group]
            if absolute:
                data = [float(c.probabilities[group]) * c.key_count for c in history.classifications]
            else:
                data = [float(c.probabilities[group]) for c in history.classifications]

            color = colors[group]

            for s in segments:
                extra = covered[s[0]]
                alpha = 0.75
                if extra:
                    alpha = 0.25
                line, = plt.plot(dates[s[0]:s[1]], data[s[0]:s[1]],
                                 label="{0}: {1}".format(group, sources_string),
                                 marker=markers[i % len(markers)],
                                 linestyle=line_styles[i % len(line_styles)],
                                 color=color,
                                 linewidth=1, alpha=alpha, markersize=markersize)
                if group not in plotted:
                    plotted[group] = line
            i += 1
        covered_dates.append((min(dates), max(dates)))

    handles = [plotted[x] for x in groups]

    if half_size:
        for handle in handles:
            handle.set_label(handle.get_label()[0:8].replace("Group ", "").replace(" ", ""))
        legend = plt.legend(handles=handles, loc='upper center',
                            bbox_to_anchor=(0.48, -0.165), ncol=7)
    else:
        # dummy stuff to balance the legend
        empty, = plt.plot(0, 0, color="white", alpha=0, label=" ")
        handles.append(empty)
        handles.append(empty)
        if len(groups) >= 13:
            handles.append(empty)
        legend = plt.legend(handles=handles, loc='upper center', bbox_to_anchor=(0.48, -0.1),
                            ncol=2, fancybox=False, shadow=False, handlelength=3,
                            frameon=False)

    if len(histories) > 1:
        i = 0
        for history in histories:
            dates = [c.timestamp for c in history.classifications]
            source = history.source
            start = min(dates)
            stop = max(dates)
            plt.axvline(start, color="g", alpha=0.5)
            plt.axvline(stop, color="r", alpha=0.5)
            height = (0.23 + i * 0.05) * max_keys
            color = "black"
            plt.plot([start, stop], [height, height], color=color)
            plt.text(start + delta, height + 0.01 * max_keys,
                     beautify_name[source] if source in beautify_name else source)
            i += 1

    if absolute and source_filename == "all_samples":
        note_color = "gray"
        date_fix = datetime.date(2015, 6, 30)
        date_fail = datetime.date(2015, 12, 30)
        ax.arrow(date_fix, 3000000, 0, -200000,
                 head_width=20, head_length=200000, color=note_color)
        plt.text(date_fix - datetime.timedelta(days=8.5 * 30), 3100000,
                 "Sonar fixed TLS 1.2 handshake",
                 color=note_color).set_fontsize(10)

        ax.arrow(date_fail, 5900000, 0, 200000,
                 head_width=20, head_length=200000, color=note_color)
        plt.text(date_fail - datetime.timedelta(days=4.5 * 30), 5600000,
                 "Unfinished Sonar scan",
                 color=note_color).set_fontsize(10)

    extra_name = "small_" if half_size else ""
    if absolute:
        fig.savefig("{0}_{1}absoluteintime.pdf".format(source_filename, extra_name))
    else:
        fig.savefig("{0}_{1}percentageintime.pdf".format(source_filename, extra_name))
    plt.close(fig)


class GraphType(Enum):
    PROFILE = 1
    STACKED = 2
    MISCLAS = 3
    DISTRIBUTION = 4

    def __str__(self):
        if self == GraphType.PROFILE:
            return 'profile'
        if self == GraphType.STACKED:
            return 'stacked'
        if self == GraphType.MISCLAS:
            return 'misclass'
        if self == GraphType.DISTRIBUTION:
            return 'distr'
        return 'graph'


def stacked_graph(source, table, classification, colors,
                  graph_type=GraphType.STACKED, main_group=None):
    if graph_type not in GraphType:
        return None
    if graph_type == GraphType.STACKED or graph_type == GraphType.DISTRIBUTION:
        fig = plt.figure(figsize=(5, 3))
        plt.subplots_adjust(left=0.13, bottom=0.14, right=0.99, top=0.91,
                        wspace=0.2, hspace=0.5)
    else:
        fig = plt.figure(figsize=(2.5, 1.5))
        plt.subplots_adjust(left=0.22, bottom=0.15, right=0.98, top=0.85,
                            wspace=0.2, hspace=0.5)
    ax = plt.axes()

    masks = table.masks
    bottoms = [0 for _ in table.masks]
    groups_and_probs = []

    if (graph_type == GraphType.PROFILE or graph_type == GraphType.MISCLAS) \
            and main_group is None:
        print('Define the group of interest')
        return None

    distribution = None
    keys_total = 1
    if graph_type == GraphType.PROFILE:
        groups_to_draw = [main_group]
        prior = dict({(g, 0) for g in table.groups})
        prior[main_group] = 1.0
    elif graph_type == GraphType.STACKED:
        groups_to_draw = table.groups
        prior = classification.probabilities
        distribution = classification.frequencies
        keys_total = classification.key_count
    elif graph_type == GraphType.DISTRIBUTION:
        groups_to_draw = []
        prior = None
        distribution = classification.frequencies
        keys_total = classification.key_count
    elif graph_type == GraphType.MISCLAS:
        groups_to_draw = classification.keys()
        prior = classification
        distribution = table.group_mask_count[main_group]
    for group in groups_to_draw:
        mask_count = table.group_mask_count[group]
        size = sum(mask_count.values())
        frequencies = [100 * float(prior[group]) * (mask_count[m] / size) if m in mask_count else 0 for m in masks]
        groups_and_probs.append((group, float(prior[group])))
        plt.bar(range(0, len(masks)),
                frequencies,
                bottom=bottoms,
                color=colors[group])
        bottoms = [bottoms[i] + frequencies[i] for i in range(len(frequencies))]
    plt.xticks([0, 64, 128, 192, 255], [0, 64, 128, 192, 255])
    groups_and_probs.sort(key=lambda x: x[1], reverse=True)
    groups_and_probs = [x for x in groups_and_probs if x[1] >= 0.001]
    patches = [mpatches.Patch(color=colors[x[0]],
                              label="{0}: {1:3.2f}%"
                              .format(x[0].replace("  ", "   "), x[1] * 100))
               for x in groups_and_probs]
    if distribution is not None:
        frequencies = [distribution[m] if m in distribution else 0 for m in masks]
        size = sum(frequencies)
        frequencies = [100 * (f / size) for f in frequencies]

        if graph_type == GraphType.MISCLAS:
            frequencies.append(0)
            frequencies.insert(0, 0)
            x_coords = list(range(0, len(masks)))
            x_coords.append(len(masks) - 1)
            x_coords.insert(0, 0)
            original_line, = plt.plot(x_coords,
                                      frequencies, color="darkgreen",
                                      alpha=0.7, label=main_group, linewidth=0.8)
            plt.title("{0} approximated".format(main_group))
        elif graph_type == GraphType.DISTRIBUTION:
            original_line = plt.bar(range(0, len(masks)), frequencies, color="black", alpha=0.7)
            plt.title(source.replace("_", " "), fontsize=16)
        else:
            original_line, = plt.plot(range(0, len(masks)), frequencies, color="black",
                                      alpha=0.7, label="Original distribution",
                                      linestyle="--", linewidth=1.5)
            plt.title(source.replace("_", " "), fontsize=16)
        patches.insert(0, original_line)
    else:
        plt.title("{0}".format(main_group))

    if keys_total > 1:
        legend_title = "Total keys: {0:,}".format(keys_total)
    else:
        legend_title = None
    if graph_type == GraphType.STACKED:  # graph_type == GraphType.MISCLAS or
        plt.legend(handles=patches, title=legend_title, loc='upper right')
    plt.xlabel("Mask")

    if keys_total == 1:
        plt.ylabel("Mask probability [%]")
    else:
        plt.ylabel("Number of keys with a given mask")
        max_keys = max(distribution.values())

        to_absolute_formatter = FuncFormatter(
            lambda x, pos: "{0}K".format(int(round(keys_total * x / 100 / 1000))))
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
                lambda x, pos: "{0:1.1f}M".format(keys_total * x / 100 / 1000000))
            ax.yaxis.set_major_formatter(to_absolute_formatter)
    source_filename = source.replace("/", "_").replace(" ", "")
    fig.savefig("{0}_{1}.pdf".format(source_filename, graph_type))
    plt.close(fig)


def color_assignment(table):
    if len(table.group_sources) == 13:
        cs = plt.cm.Vega20(np.linspace(0, 1, 20))
        color_sequence = [cs[1], cs[3], cs[5], cs[7], cs[9], cs[11],
                          cs[4], cs[17], cs[12], cs[15], cs[18], cs[6], cs[2]]
    elif len(table.group_sources) == 12:
        cs = plt.cm.Vega20(np.linspace(0, 1, 20))
        color_sequence = [cs[1], cs[3], cs[5], cs[7], cs[9], cs[11],
                          cs[4], cs[12], cs[15], cs[18], cs[6], cs[2]]
    else:
        color_sequence = plt.cm.Vega20(np.linspace(0, 1, len(table.group_sources)))

    colors = dict()
    groups = table.groups
    for i in range(0, len(groups)):
        color = color_sequence[i % len(color_sequence)]
        colors[groups[i]] = color
    return colors


def main():
    parser = argparse.ArgumentParser(description='Visualization of classification')
    parser.add_argument("-t", "--table", action="store", required=True,
                        help="CSV classification table (Java tool \"-ec in_table.json out_table.csv\")")
    parser.add_argument("-n", "--name", action="store",
                        help="Name of the domain", default=None)
    parser.add_argument("-m", "--misclass", action="store",
                        help="Misclassification result JSON", default=None)
    parser.add_argument("dirs", nargs=argparse.REMAINDER,
                        help="Top directories (contained dirs must contain a date or a timestamp in the name)")

    args = parser.parse_args()

    table_name = args.table
    source = args.name
    misclass_name = args.misclass
    directories = args.dirs

    table = Table.from_csv(table_name)

    colors = color_assignment(table)

    matplotlib.rcParams['pdf.fonttype'] = 42
    matplotlib.rcParams['ps.fonttype'] = 42

    if len(directories) > 0:
        histories = []
        for directory in directories:
            history = DatasetHistory.from_directory(directory)
            histories.append(history)
        draw_history(source, table, histories, colors, half_size=False, absolute=True)
        draw_history(source, table, histories, colors, half_size=True, absolute=True)
        stacked_graph(source, table,
                      histories[-1].classifications[-1], colors)
        stacked_graph(source, table,
                      histories[-1].classifications[-1], colors,
                      GraphType.DISTRIBUTION)
    else:
        if misclass_name is not None:
            f = open(misclass_name, "r")
            miss = json.load(f)
            f.close()
            for group in table.groups:
                stacked_graph(group, table, miss[group],
                              colors, GraphType.MISCLAS, group)

        for group in table.groups:
            stacked_graph(group, table, None, colors,
                          GraphType.PROFILE, group)


if __name__ == "__main__":
    main()
