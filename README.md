# Roundtrip

Roundtrip is a limited port of OpenAPS (author Ben West) into an Android application.  This application is highly experimental and potentionally quite dangerous.  It has been shared with fellow OpenAPS researchers who have already demonstrated equalivalent capability and the ability to appropriately use the software with appropriate safety precautions.  As members of the OpenAPS community we share a common goal to enhance APS technologies and help bring about more timely advances in the treatment of diabetes.  Please do not share or distribute this software with anyone else. </br>

For a current list of capability, goals, and progress please ask to join our Trello board.  Email me at toby@canning.us

### Bluetooth TODO

Connecting Roundtrip to RileyLink to send the data to Nightscout.

- Led indicators on RileyLink
  - BLE113
    - Blue: Packets received and ready for a phone to pick up
    - Green: Connected to a phone
  - CC1110
  	- Blue: Timer based on/off
  	- Green: Incomming package
  - Battery
    - Red: Charging the battery
- TODO: After the initial device discovery, save the device’s MAC address. See BluetoothDevice.getAddress(). Then, next time the user wants to connect, use that address to construct the BluetoothDevice object. See BluetoothAdapter.getRemoteDevice(). This will bypass device discovery which can create a smoother user experience.


