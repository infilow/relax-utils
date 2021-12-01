# relax-utils

Simple utils to relax my life. See the [documents](https://relax.infilos.com/relax-utils/index.html) for more detail...

## Contributions

### Release

- Snapshot: `mvn clean deploy`
- Release: `mvn clean package source:jar gpg:sign install:install deploy:deploy`
  - sonatype nexus: close && release
