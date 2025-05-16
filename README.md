# json-test-plugin

<!-- Plugin description -->
Allows to run/debug bazel tests defined via json files. Relies on the existing bazel test Run Configuration that should be saved as `Bazel Test Template`.
<!-- Plugin description end -->

Test files should adhere the following structure:

```
{
  "tests": [
    { name: "test1", ... },
    { name: "test2", ... }
  ]
}
```

## Installation

-  Download the [latest release](https://github.com/sqshq/json-test-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
- Run the desired test once, click <kbd>Run</kbd> > <kbd>Edit Configurations</kbd> and save the last run as  `Bazel Test Template`
- Make a change to your test to accept the `json.test.prefix` env variable to filter the tests based on the provided `filename_testname` prefix

## Next steps

- Allow support for multiple json tests per project, possibly utilizing BAZEL build file property to point plugin to the relevant test class
- Add shortcut support
