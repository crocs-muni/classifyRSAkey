python3 ../../../visualization/visualize_dataset.py -t ../csv_ACSAC_Public_mod3_mod4_N2-7.csv -m ../misclassification.json

python3 ../../../visualization/explain_mask.py ../csv_ACSAC_Public_mod3_mod4_N2-7.csv "Group  7"
Rscript ../../../visualization/draw_dendrogram.r ../raw_ACSAC_Public_mod3_mod4_N2-7.csv
