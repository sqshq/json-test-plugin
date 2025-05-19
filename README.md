# json-test-plugin

<!-- Plugin description -->
IntelliJ IDEA plugin that allows to run/debug bazel tests defined as json files. Relies on the existing bazel test Run Configuration that should be saved as `Bazel Test Template`.
<!-- Plugin description end -->

Skunkworks project. Tested only on IntelliJ v2025.1.1, use at your own risk.

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
- Run the desired test once, click <kbd>Run</kbd> > <kbd>Edit Configurations</kbd> and save the last run as  `Bazel Test Template`, make sure to check `Store as project file`:
  ![output](https://github.com/user-attachments/assets/669d370d-4af1-4595-8bc3-681ad055e6d1)


## Next steps

- Add support for multiple run configurations per project, possibly utilizing BAZEL build file property to point plugin to the relevant test class
- Add support for shortcuts
