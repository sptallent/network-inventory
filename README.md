# Network Inventory - Device Discovery Android Application

This repository contains an Android application written in native Java that is designed to discover devices on a network. Please note that this project no longer works due to security changes in API 29. It will be updated in the future to accommodate the changes and support devices with the updated APIs when I find the time to work around these changes.

## Project Description

Network Inventory utilizes various scanning techniques to identify devices on the network. It scans the available subnets and services using the following protocols:

- mDNS (Multicast DNS): A custom mDNS implementation is included in this project, which handles mDNS packets based on the RFC 6762 documentation.
- NBNS (NetBIOS Name Service): Scans for devices using the NBNS protocol.
- UPnP (Universal Plug and Play): Searches for devices that support UPnP.
- SNMP (Simple Network Management Protocol): Discovers devices using SNMP.

## Usage

1. Clone the repository to your local machine. ``git clone https://github.com/sptallent/network-inventory.git```
2. Open the project in Android Studio.
3. Build and run the application on an Android device or emulator.

Please note that due to the changes in API 29, the current version of this application will not work as expected. Future updates might be provided to address these security changes and ensure compatibility with devices utilizing the updated APIs.

## Contributing

Contributions to this project are welcome. If you would like to contribute, please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Commit your changes and push them to your fork.
4. Submit a pull request, explaining your changes and why they should be merged.

## License

This project is licensed under the [MIT License](LICENSE). Please see the LICENSE file for more details.

## Contact

If you have any questions or suggestions regarding this project, feel free to contact the project maintainer at samuel.tallent00@gmail.com
