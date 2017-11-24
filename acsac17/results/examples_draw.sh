cd examples

python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n "TLS_03/2017_(Censys)" ../https/tls/443-https-tls-full_ipv4-20170311T081209-zgrab-results.json.lz4.cl.json
python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n "Certificate_Transparency_03/2017" ../ct_together/together_month/1490486400000.json
python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n "TLS_03/2017_Alexa_Top_1M_(Censys)" ../https/alexa/443-https-tls-alexa_top1mil-20170329T100001-zgrab-results.json.lz4.cl.json

python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n "PGP_04/2017" ../pgp/2017-04-19-pgp_classif_full.json
python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n "GitHub_02/2017" ../github/*
python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n "TLS_08/2010_(EFF_SSL_Observatory)" ../https/eff/2010-08-15-eff-august.json.unique.json
python3 ../../../visualization/visualize_dataset.py -t ../../table/csv_ACSAC_Public_mod3_mod4_N2-7.csv -n "TLS_12/2010_(EFF_SSL_Observatory)" ../https/eff/2010-12-15-eff-december.json.unique.json

