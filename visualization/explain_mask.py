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

if len(sys.argv) < 2:
    print("Usage: {0} classification_table.csv \"group_name\"".format(
        sys.argv[0]))
    print("  To obtain the classification table in csv format from json,\n"
          "    use the \"-ec in_table.json out_table.csv\" option of the classification tool")
    print("  By default, \"Group  7\" will be illustrated")
    exit(1)

table_name = sys.argv[1]
group = "Group  7" if len(sys.argv) < 3 else sys.argv[2]

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

f.close()

fig = plt.figure(figsize=(3, 2.5))
plt.subplots_adjust(left=0.20, bottom=0.2, right=0.99, top=0.9,
                                wspace=0.2, hspace=0.5)

mask_count = group_mask_count[group]
frequencies = [mask_count[m] if m in mask_count else 0 for m in masks]
size = sum(frequencies)
frequencies = [100 * (mask_count[m] / size) if m in mask_count else 0 for m in masks]
plt.bar(range(0, len(masks)),
        frequencies, color="grey", alpha=0.5)

alpha = 0.5

ticks = [0, 64, 128, 192, 255]

plt.xticks(ticks, ticks)
plt.gca().set_ylim(bottom=0, top=2.7)
plt.title("Feature mask")
plt.ylabel("Mask probability [%]")
plt.xlabel("Mask value")

a = "#22AA22"
b = "#0008F0"
c = "#AA6600"
d = "#0066AA"

line = 2.4
color = "green"
color2 = "blue"
plt.axvline(0, alpha=alpha, color=a)
plt.axvline(64, alpha=alpha, color="red", ymax=0.86)
plt.axvline(128, alpha=alpha, color="black")
plt.axvline(192, alpha=alpha, color="red", ymax=0.86)
plt.axvline(255, alpha=alpha, color=b)
fontsize = 12
pad = 5

plt.text(0 + pad, line, "N % 3 == 1", fontsize=fontsize, color=a)
plt.text(128 + pad, line, "N % 3 == 2", fontsize=fontsize, color=b)
line2 = line - 0.5
fontsize2 = 10
pad2 = pad / 2

plt.text(0 + pad2, line2, " N % 4 \n  == 1", fontsize=fontsize2, color=c)
plt.text(64 + pad2, line2, " N % 4 \n  == 3", fontsize=fontsize2, color=d)
plt.text(128 + pad2, line2, " N % 4 \n  == 1", fontsize=fontsize2, color=c)
plt.text(192 + pad2, line2, " N % 4 \n  == 3", fontsize=fontsize2, color=d)

fontsize3 = 9
for offset in [0, 64, 128, 192]:
    plt.text(offset + 4, 1.5, "N = 1000000...", rotation=90, fontsize=fontsize3)
    plt.text(offset + 24, 1.5, "N = 1100000...", rotation=90, fontsize=fontsize3)
    plt.text(offset + 45, 1.5, "N = 1111111...", rotation=90, fontsize=fontsize3)

fig.savefig("explain_mask.pdf")
