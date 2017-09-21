cd generated

python3 ../../../visualization/visualize_dataset.py ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv ct_le_live_7_recent ../ct_letsencrypt_recent/le_unique_split_live_7/*
python3 ../../../visualization/visualize_dataset.py ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv ct_le_live_3_recent ../ct_letsencrypt_recent/le_unique_split_live_3/*

python3 ../../../visualization/visualize_dataset.py ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv "Certificate_Transparency_(all_05/2017)" ../ct_together/live_unique_3/*
python3 ../../../visualization/visualize_dataset.py ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv "Certificate_Transparency_(05/2017)" ../ct_together/live_unique_7/*

python3 ../../../visualization/visualize_dataset.py ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv all_samples ../https/eff/* ../https/eco/* ../https/tls/*  ../https/sonar/*
python3 ../../../visualization/visualize_dataset.py ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv alexa ../https/alexa/*

python3 ../../../visualization/visualize_dataset.py ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv selfsigned ../selfsigned/true/eco/* ../selfsigned/true/sonar/*
python3 ../../../visualization/visualize_dataset.py ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv not_selfsigned ../selfsigned/false/eco/* ../selfsigned/false/sonar/*
