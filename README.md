## SearchQL generator

Generator for search query language. 
Generator scheme and language queries examples can be found in [this](https://github.com/ilyavoronin/gen-search-ql-tc-example) repository

### Run generator
```shell
./gradlew run --args='<scheme_path> <folder_path> <package>'
```
`scheme_path` - path to the generator scheme  
`folder_path` - path to the folder where the code should be generated  
`package` - package for generated code

### Build jar
```shell
./gradlew buildGenerator
```

### Run tests
```shell
./gradlew test
```