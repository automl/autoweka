How to make a release
=====================

Preparation
-----------

* Update the version, date and URL in `Description.props` to reflect new
  version, e.g.:

  ```
  Version=2.6.2
  Date=2021-01-20
  PackageURL=https://github.com/fracpete/autoweka/releases/download/v2.6.2/autoweka-2.6.2.zip
  ```

* Commit/push all changes


Weka package
------------

* Run the following command to generate the package archive for version
  `2.6.2`:

  ```
  ant clean test weka-package
  ```

* create a release tag on github (`v2.6.2`)
* add release notes
* upload package archive from root directory
* add link to this zip file in the `Releases` section of the `README.md` file

