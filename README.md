# SBOMs

Long term storage of build provenance files / software bills of material (SBOM)

## How reports are generated?

### Sonatype-Lift

1. Go to [sonatype lift console](https://lift.sonatype.com/results/github.com/SpoonLabs/sorald/01GF3EZ99T224KCGHP20Y32671?tab=dependencies)
   and click the "dependenices" tab.
2. Click "Export CycloneDX" button to export the SBOM in JSON format.

### Renovate

1. Inspired from Renovate's ["Detected dependecies"](https://github.com/SpoonLabs/sorald/issues/623).
2. Run the following command and you will get the list of
   "Detected dependencies" in STDOUT and the log file.
   ```bash
   renovate --token [REDACTED] --dry-run="extract"  --autodiscover --autodiscover-filter "<org_name>/<repo_name>" --log-file="renovate.log"
   ```
   One may use Renovate's npm package or docker image to the run the
   above command.
3. See https://github.com/renovatebot/renovate/discussions/18258#discussion-4463299 for more information.
