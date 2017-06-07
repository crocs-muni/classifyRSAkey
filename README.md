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

#### Create a classification table from raw keys
```
java -jar classifyRSAkey.jar -m in_makefile.json out_table.json
```
* in_makefile.json - makefile which specifies the feature mask and the path to source keys
  * see example: TODO
  * format of keys: csv file, with header ```id;n;e;p;q;d;t``` (id (dec), modulus (hex), public exponent (hex), prime1 (hex), prime2 (hex), private exponent (hex), time of generation (dec, can be 0))
* out_table.json - primary classification table
  * see example: TODO
  
Raw keys:
* RSA keys from software libraries: https://drive.google.com/folderview?id=0B0PpUrsKytcyUUV5d3kwX0VRNFk&usp=sharing
  * Separate zip files for every library and length of RSA keys. Naming format: //library_version_keylength.zip// 
* RSA keys from cryptographic smartcards: https://drive.google.com/open?id=0B_DMu_2XOQ9XQWYyQmxXbDZuems
  * Separate zip files for every library and length of RSA keys. Format: //smartcard-numberOfKeys-keyLength.zip//

#### Remove duplicate moduli in a dataset of public keys
```
java -jar classifyRSAkey.jar -rd in_rsa_keys.json out_rsa_unique_keys.json
```
* in_rsa_keys.json - RSA public keys, one JSON object per line
  * ```{"n":"<RSA modulus in hexadecimal>", "source":[<list of strings, e.g. validity, common name>]}```
* out_rsa_unique_keys.json - unique RSA keys
  * adds ```"occurrence":<number of duplicities>```, collects source information

#### Classification of keys, produces summary
```
java -jar classifyRSAkey.jar -c in_table.json in_rsa_unique_keys.json out_dir
```
* in_table.json - classification table, output of -m
* in_rsa_unique_keys.json - unique RSA keys, one JSON object per line, output of -rd
* out_dir - directory for output
  * dataset.json - copy of the dataset, with added properties ```
"batch":<id of batch - same source>, "vector":<feature mask of the key>, "probabilities":[<list of probabilities by group>]}```
  * "results, x - y keys.csv" - summary results, x - y is the minimum and maximum size of a batch
    * Results of classification of each key are summed as vectors, the resulting vector is normalized to sum to 1
      * Positive - summary for all keys for the given group
      * Positive, Unique - summary for all unique keys for the given group
    * Number of keys which are certainly not coming from a given source
      * Negative - number of all negative results for the given group
      * Negative, Unique - number of unique negative results for the given group

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
