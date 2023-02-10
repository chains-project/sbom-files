## Format unifier

The CLI tools converts each SBOM format to an abstract JSON.
```json
[
    {
        "groupId": "com.nimbusds",
        "artifactId": "lang-tag",
        "classifier": "jar",
        "version": "1.6",
        "scope": "provided",
        "depth": 4,
        "submodule": "org.mybatis:mybatis:jar:3.5.11"
    },
]
```

### Usage

1. Install the dependencies in the pip file.
2. Run command as suggsted in help.
    ```sh
    usage: main.py [-h] -s {cyclonedx,spdx,jbom} -i INPUT [-o OUTPUT]

    options:
    -h, --help            show this help message and exit
    -s {cyclonedx,spdx,jbom}, --spec-type {cyclonedx,spdx,jbom}
                            choose from the specified set
    -i INPUT, --input INPUT
                            SBOM file to be converted
    -o OUTPUT, --output OUTPUT
                            path to output file. defaults to stdout.
    ```

    Example run:
    ```sh
    python main.py -s cyclonedx -i /home/aman/chains/sbom-files/mybatis-3/cyclonedx-maven-plugin/bom.json -o transformed-bom.json
    ```
