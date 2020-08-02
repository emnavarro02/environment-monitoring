# BLE DataCollector Bundle App

**OSGi Bundle** application that uses **TinyB** to collect temperature, pressure, height and battery level from BobXia sensors beacons through Bluetooth Low Energy.

This app has been developed to run over the OSGi Framework and was tested in **Apache Felix 6.0.2**.

These apps retrieve data from sensors and publishes it to a Publish/Subscriber (in Apache Felix it's named Event Admin Service).

## Useful links:
- TinyB Documentation: https://github.com/intel-iot-devkit/tinyb
- Apache Felix Event Admin: http://felix.apache.org/documentation/subprojects/apache-felix-event-admin.html
- OSGi Compendium Release 7: https://osgi.org/specification/osgi.cmpn/7.0.0/service.event.html#d0e47273
