# Roundtrip

Roundtrip is a limited port of OpenAPS (author Ben West) into an Android application.  

Goals:
1. Receive/transmit data from Medtronic Pumps.
2. Receive Dexcom CGM date via bluetooth.
3. Implement the "reference design" artificial pancraes software as designed by Dana Lewis and Scott Leibrand (openaps.org).
4. Upload relevant treatment decisions made by APS to Nightscout.
5. Use a device like the xDrip (author Stephen Black) and Rileylink (https://github.com/ps2/rileylink, author Pete Schwamb) to tranmit/receive data from Android app to pump.
6. Have a clean user interface for controlling the APS.

Current Progress (As of 1 June 2015):
1. Roundtrip currently uses the CareLink stick connected to the phone via USB OTG to transmit/receive data to/from pump.
2. The app can currently retreive pump history, but is limited to the first page. It can separate the history data.
3. The app can set temp basals.
4. Much of the APS logic for making automating corrections is done.  This has been tested on a limited basis in a prototype device done in python on the Raspberry Pi.
5. CGM data can currently be read from a mongo database.

To Do:
1. Complete the app's ability to retreive and parse necessary pump history required for automated corrections. Ideally, the setting specificied in the pump itself will be used for insulin sensitivy, BG limits, correction factor, max basal rate, and basal rate profile.
2. Finish the OpenAPS logic.
3. Add a UI.  Include the ability to turn program on/off, suspend APS for x minutes and resume, set target BG, specify DIA (if different setting than one in pump is needed), and specify any other settings required but not retreived from pump.
