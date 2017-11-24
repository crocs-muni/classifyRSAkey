cd generated

python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n ct_le_live_7_recent ../ct_letsencrypt_recent/le_unique_split_live_7/
python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n ct_le_live_3_recent ../ct_letsencrypt_recent/le_unique_split_live_3/

python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n all_samples ../https/eff/ ../https/eco/ ../https/tls/  ../https/sonar/
python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n alexa ../https/alexa/

python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n selfsigned ../selfsigned/true/eco/ ../selfsigned/true/sonar/
python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n not_selfsigned ../selfsigned/false/eco/ ../selfsigned/false/sonar/
