#!/usr/bin/env python3

import json
import os
import matplotlib.pyplot as plt
import matplotlib.dates as mdates
import sys
import math
from re import findall
import datetime
import csv
import numpy as np
import matplotlib.patches as mpatches
from matplotlib.ticker import MultipleLocator, FormatStrFormatter, LogLocator, \
    FuncFormatter, LogFormatter

if len(sys.argv) != 3:
    print("Usage: {0} classification_table.csv misclassification.json".format(
        sys.argv[0]))
    print("  To obtain the classification table in csv format from json,\n"
          "    use the \"-ec in_table.json out_table.csv\" option of the classification tool")
    print("  To obtain a misclassification result,\n"
          "    use the \"-mc -t in_table.json > miss.json\" option of the classification tool")
    exit(1)

table_name = sys.argv[1]
misclassification_filename = sys.argv[2]

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

groups = group_mask_count.keys()

if len(groups) == 13:
    # preferred colors for the 13 groups
    cs = plt.cm.Vega20(np.linspace(0, 1, 20))
    color_sequence = [cs[1], cs[3], cs[5], cs[7], cs[9], cs[11],
                      cs[4], cs[17], cs[12], cs[15], cs[18], cs[6], cs[2]]
else:
    color_sequence = plt.cm.Vega20(np.linspace(0, 1, len(groups)))

colors = dict()

i = 0
for g in sorted(group_mask_count.keys()):
    colors[g] = color_sequence[i]
    i += 1

f = open(misclassification_filename, "r")
miss = json.load(f)
f.close()

cols = 5
rows = 3

for single in [False]:  # [False, True]:  # make a single file or multiple files
    for misclassified in [False, True]:
        if single:
            fig = plt.figure(figsize=(4 * cols, 2 * rows))
        i = 0
        for g in sorted(group_mask_count.keys()):
            i += 1
            if single:
                sub = fig.add_subplot(rows, cols, i)
                plt.subplots_adjust(left=0.05, bottom=0.05, right=0.95, top=0.95,
                                    wspace=0.2, hspace=0.5)
            else:
                fig = plt.figure(figsize=(2.5, 1.5))
                plt.subplots_adjust(left=0.22, bottom=0.15, right=0.98, top=0.85,
                                    wspace=0.2, hspace=0.5)

            print(g)
            plt.xticks([0, 64, 128, 192, 255], [0, 64, 128, 192, 255])
            frequencies = [
                group_mask_count[g][m] if m in group_mask_count[g] else 0 for m
                in masks]
            size = sum(frequencies)
            frequencies = [100 * (f / size) for f in frequencies]
            if misclassified:
                bottoms = [0 for x in masks]
                for group in miss[g]:
                    mask_count = group_mask_count[group]
                    other_frequencies = [mask_count[m] if m in mask_count else 0 for m in masks]
                    size = sum(other_frequencies)
                    other_frequencies = [100 * float(miss[g][group])
                                         * (mask_count[m] / size)
                                         if m in mask_count
                                         else 0 for m in masks]
                    print(miss[g][group])
                    plt.bar(range(0, len(masks)),
                            other_frequencies,
                            bottom=bottoms,
                            color=colors[group])
                    bottoms = [bottoms[i] + other_frequencies[i]
                               for i in range(len(other_frequencies))]
                frequencies.append(0)
                frequencies.insert(0, 0)
                x_coords = list(range(0, len(masks)))
                x_coords.append(len(masks) - 1)
                x_coords.insert(0, 0)
                original_line, = plt.plot(x_coords,
                                          frequencies, color="darkgreen",
                                          alpha=0.7, label=g, linewidth=0.8)
                plt.title("{0} approximated".format(g))
            else:
                plt.bar(range(0, len(masks)), frequencies, color=colors[g])
                plt.title(g)

            misclass_legend = False

            if misclass_legend and misclassified and not single:
                sorted_mis_groups = [(group, float(miss[g][group])) for group in miss[g]]
                sorted_mis_groups.sort(key=lambda x: x[1], reverse=True)
                sorted_mis_groups = [x for x in sorted_mis_groups if x[1] >= 0.01]
                patches = [mpatches.Patch(color=colors[item[0]],
                                          label="{0}: {1:3.1f}%"
                                          .format(int(item[0].replace("Group ", "").replace(" ", "")),
                                                  item[1] * 100))
                           for item in sorted_mis_groups]
                patches.insert(0, original_line)
                plt.legend(handles=patches, title="Groups", loc='upper right')
            if not single:
                ax = plt.axes()
                max_percentage = max(frequencies)
                if max_percentage < 1:
                    ax.yaxis.set_major_locator(MultipleLocator(0.2))
                    ax.yaxis.set_minor_locator(MultipleLocator(0.1))
                elif max_percentage < 3:
                    ax.yaxis.set_major_locator(MultipleLocator(0.5))
                    ax.yaxis.set_minor_locator(MultipleLocator(0.1))
                elif max_percentage < 10:
                    ax.yaxis.set_major_locator(MultipleLocator(1))
                    ax.yaxis.set_minor_locator(MultipleLocator(0.5))
                else:
                    ax.yaxis.set_major_locator(MultipleLocator(2))
                    ax.yaxis.set_minor_locator(MultipleLocator(1))

            plt.ylabel("Mask probability [%]")
            if not single:
                if misclassified:
                    fig.savefig("misclass_{0}.pdf".format(g.replace(" ", "")))
                else:
                    fig.savefig("profile_{0}.pdf".format(g.replace(" ", "")))
                plt.close(fig)
        if single:
            if misclassified:
                fig.savefig("misclassified.pdf")
            else:
                fig.savefig("profiles.pdf")
            plt.close(fig)