# gsonfilereference

Adds the possibility to have file references in Json files to Gson.

Strings, ints, arrays and objects that are defined as a string starting with file:// are considered references to other json (or content) files.

It could be worth considering implementing http://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03 for getting references, instead of just using the value as string.
