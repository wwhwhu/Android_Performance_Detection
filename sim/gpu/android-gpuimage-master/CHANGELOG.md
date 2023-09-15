Change Log
==========

Version 2.1.0 *(2020-09-21)*
----------------------------

* Update
  * Kotlin to 1.3.72
  * The targetSdkVersion from 28 to 30
  * The compileSdkVersion from 28 to 30

Version 2.0.4 *(2019-08-13)*
----------------------------

* Update
  * Kotlin to 1.3.41
  * Android Gradle tools to 3.6.0-alpha05
  * Gradle wrapper to 5.5

* Add
  64bit build settings for clearly
  Can get size of rescaled image [#443](https://github.com/cats-oss/android-gpuimage/pull/443)
        
* Bug fix
  GPUImageZoomBlurFilter incorrect args [#454](https://github.com/cats-oss/android-gpuimage/pull/454)

Version 2.0.3 *(2018-11-09)*
----------------------------

* Add GPUImageVibranceFilter (by @itome)

Version 2.0.2 *(2018-11-01)*
----------------------------

* Add GPUImageSolarizeFilter (by @kettsun0123)

* Change attr/names
  `show_loading` to `gpuimage_show_loading`
  `surface_type` to `gpuimage_surface_type`

* Fix a bug about filter init [#420](https://github.com/cats-oss/android-gpuimage/pull/420)

Version 2.0.1 *(2018-10-24)*
----------------------------

* Add GPUImageLuminanceFilter (by @takasfz)
* Add GPUImageLuminanceThresholdFilter (by @takasfz)

Version 2.0.0 *(2018-10-23)*
----------------------------

* Change the minSdkVersion 9 to 14
* Change the targetSdkVersion 23 to 28
* Update project settings
* Support TextureView via GLTexureView
* Support Camera2 API
* Fix some bugs


Version 1.4.1 *(2016-03-15)*
----------------------------
 Using Bintray's JCenter.

Version 1.4.0 *(2016-02-28)*
----------------------------

* added GPUImageHalftoneFilter (by @ryohey)
* added GPUImageTransformFilter (by @jonan)
* fixed GPUImageChromaKeyBlendFilter (by @badjano)
* fixed GPUImageLookupFilter (by @jonan)

Version 1.3.0 *(2015-09-04)*
----------------------------

* added GPUImageBilateralFilter (by @wysaid)
* added flip options to `GPUImage#setRotation`

Version 1.2.3-SNAPSHOT *(2014-12-15)*
----------------------------

* added GPUImageLevelsFilter (by @vashisthg)
