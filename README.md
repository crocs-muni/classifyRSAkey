## The Million-Key Question – Investigating the Origins of RSA Public Keys
### RSA key classification tool

Java tool for classification of RSA keys based on subtle biases in different key generation methods used by cryptographic libraries.

Project details: https://crocs.fi.muni.cz/public/papers/usenix2016

Conference website: https://www.usenix.org/conference/usenixsecurity16/technical-sessions/presentation/svenda

#### Build
```
git clone git@github.com:crocs-muni/classifyRSAkey.git
cd classifyRSAkey/
ant
```

#### Run
```
cd out/artifacts/classifyRSAkey_jar/
java -jar classifyRSAkey.jar
```

### Basic usage

#### Classification of keys, produces summary
```
java -jar classifyRSAkey.jar -c in_classification_table.json in_rsa_unique_keys.json out_dir
```
* in_classification_table.json - classification table, output of -m
  * see also [precomputed classification table for USENIX Security '16](usenixsecurity16/classificationTable.json)
* in_rsa_unique_keys.json - unique RSA public keys, one JSON object per line
  * ```{"n":"0x<RSA modulus in hexadecimal>", "e":"0x<public exponent in hexadecimal>", "source":[<list of strings, e.g., validity, common name>]}```
  * if the original dataset contains duplicate RSA moduli, first use -rd
  * see also [example RSA public keys](usenixsecurity16/exampleKeysForClassification.json)
* out_dir - directory for output - following files will be created:
  * dataset.json - copy of the dataset, with added properties, first line is the table as JSON string
    * ```"batch":<id of batch - same source>, "vector":<feature mask of the key>, "probabilities":[<list of probabilities by group>]```
    * mostly for machine processing
  * "results, x - y keys.csv" - summary results, x - y is the minimum and maximum size of a batch
    * Results of classification of each key are summed as vectors, the resulting vector is normalized to sum to 1
      * Positive - summary for all keys for the given group
      * Positive, Unique - summary for all unique keys for the given group
    * Number of keys which are certainly not coming from a given source
      * Negative - number of all negative results for the given group
      * Negative, Unique - number of unique negative results for the given group
  * "out_dir/containers results, x - y keys.csv" - summary results, similar as results, but aggregated over batches
* Example:
  * input file in_rsa_unique_keys.json contains: 
    * ```{"n":"0xC7794195AEB35E3A69592C05387EBCAC2D9941D1037FB6277958E2FA37B14244B54922920A0D7A2E4D0A6C8E98E843F8FD4376A1ED8ED85F5D9BAA3188351043", "e":"0x10001", "count":1, "source":["Example OpenSSL key"]}```
  * output file "containers results, 1 - 1 keys.csv" (note: batch size is 1, because there is only one key with such source) contains:
```
Source, [Example OpenSSL key]
Unique keys, 1
Keys, 1
Group  1  , Group  2  , Group  3  , Group  4  , Group  5  , Group  6  , Group  7  , Group  8  , Group  9  , Group 10  , Group 11  , Group 12  , Group 13
-         , -         , -         , 0.00471783, 0.39571325, -         , -         , 0.07717268, 0.17829864, 0.14552113, 0.19857646, -         , -
```
  * Example explanation: groups with "-" (1, 2, 3, 6, 7, 12, 13) could never generate this key, the group with the highest value (Group 5 - OpenSSL - with 0.3957) is the most probable guess, etc.

#### Show information about groups in a classification table
```
java -jar classifyRSAkey.jar -i in_classification_table.json
```
Example output for [precomputed classification table for USENIX Security '16](usenixsecurity16/classificationTable.json):
```
Group name: Group sources
Group  1: G&D SmartCafe 4.x, G&D SmartCafe 6.0
Group  2: GNU Crypto 2.0.1
Group  3: NXP J2D081, NXP J2E145G
Group  4: PGPSDK 4 FIPS
Group  5: OpenSSL 1.0.2g
Group  6: Oberthur Cosmo Dual 72k
Group  7: NXP J2A080, NXP J2A081, NXP J3A081, NXP JCOP 41 v2.2.1
Group  8: Bouncy Castle 1.53, Cryptix JCE 20050328, FlexiProvider 1.7p7, SunRsaSign (OpenJDK 1.8), mbedTLS 2.2.1
Group  9: Gemalto GXP E64
Group 10: Bouncy Castle 1.54, Crypto++ 5.6.3, Microsoft .NET, Microsoft CNG, Microsoft CryptoAPI
Group 11: Botan 1.11.29, Feitian JavaCOS A22, Feitian JavaCOS A40, GNU Libgcrypt 1.6.5, GNU Libgcrypt 1.6.5 FIPS, Gemalto GCX 72K, LibTomCrypt 1.17, Nettle 3.2, Oberthur Cosmo 64, OpenSSL FIPS 2.0.12, PGPSDK 4, WolfSSL 3.9.0, cryptlib 3.4.3
Group 12: Infineon JTOP 80K
Group 13: G&D SmartCafe 3.2
```

#### Create a custom classification table from raw keys
```
java -jar classifyRSAkey.jar -m in_makefile.json out_classification_table.json
```
* in_makefile.json - makefile which specifies the feature mask and the path to source keys
  * see [makefile for USENIX Security '16](usenixsecurity16/classificationTableMakefile.json)
  * features in feature mask extend the abstract class Transformation
  * format of keys: csv file, with header ```id;n;e;p;q;d;t``` (id (dec), modulus (hex), public exponent (hex), prime1 (hex), prime2 (hex), private exponent (hex), time of generation (dec, can be 0))
* out_classification_table.json - classification table
  * see [precomputed classification table for USENIX Security '16](usenixsecurity16/classificationTable.json)
  
Raw keys:
* RSA keys from software libraries:
  * https://drive.google.com/folderview?id=0B0PpUrsKytcyUUV5d3kwX0VRNFk&usp=sharing
  * Separate zip files for every library and length of RSA keys. Naming format: library_version_keylength.zip
* RSA keys from cryptographic smartcards:
  * https://drive.google.com/open?id=0B_DMu_2XOQ9XQWYyQmxXbDZuems
  * Separate zip files for every library and length of RSA keys. Format: smartcard-numberOfKeys-keyLength.zip

#### Remove duplicate moduli in a dataset of public keys
```
java -jar classifyRSAkey.jar -rd in_rsa_keys.json out_rsa_unique_keys.json
```
* in_rsa_keys.json - RSA public keys, one JSON object per line
  * ```{"n":"0x<RSA modulus in hexadecimal>", "e":"0x<public exponent in hexadecimal>", "source":[<list of strings, e.g., validity, common name>]}```
* out_rsa_unique_keys.json - unique RSA keys
  * adds ```"occurrence":<number of duplicities>```, collects source information

### Default output - usage help
```
RSAKeyAnalysis tool, CRoCS 2016
Options:
  -m   make  out       Build classification table from makefile.
                        make  = path to makefile
                        out   = path to json file (classification table file)
  -i   table           Load classification table and show information about it.
                        table = path to classification table file
  -ed  table out       Create table showing euclidean distance of sources.
                        table = path to classification table file
                        out   = path to html file
  -ec  table out       Convert classification table to out format.
                        table = path to classification table file
                        out   = path to csv file
  -er  table out       Export raw table (usable for generate dendrogram)
                        table = path to classification table file
                        out   = path to csv file
  -cs  table out keys  Compute classification success.
                        table = path to classification table file
                        out   = path to csv file
                        keys  = how many keys will be used for test.
  -mc  table out keys  Compute misclassification rate.
                        table = path to classification table file
                        out   = path to csv file
                        keys  = how many keys will be used for test.
  -pgp in    out       Convert pgp keys to format for classification.
                        in    = path to json file (dump of pgp key set)
                                http://pgp.key-server.io/dump/
                                https://github.com/diafygi/openpgp-python
                        out   = path to json file (classification key set)
  -f   in              Load factors which have to be try on modulus of key.
                        in    = path to txt file
                                Each line of file contains one factor (hex).
  -c   table in  out   Classify keys from key set.
                        table = path to classification table file
                        in    = path to key set
                        out   = path to folder for storing results
  -cf  table in  out   Classify private keys which share some factors
                       (factored by batch GCD) from key set.
                        table = path to classification table file
                        in    = path to key set
                        out   = path to folder for storing results
  -rd  in    out       Remove duplicity from key set.
                        in    = path to key set
                        out   = path to key set
  -nc  table           Set classification table for not classify some keys.
                        table = path to classification table file
  -h                   Show this help.
```
