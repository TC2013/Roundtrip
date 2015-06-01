# Roundtrip

Roundtrip is a limited port of OpenAPS (author Ben West) into an Android application.  

<b>Goals:</b><br />
1. Receive/transmit data from Medtronic Pumps.<br />
2. Receive Dexcom CGM date via bluetooth.<br />
3. Implement the "reference design" artificial pancraes software as designed by Dana Lewis and Scott Leibrand (openaps.org).<br />
4. Upload relevant treatment decisions made by APS to Nightscout.<br />
5. Use a device like the xDrip (author Stephen Black) and Rileylink (https://github.com/ps2/rileylink, author Pete Schwamb) to tranmit/receive data from Android app to pump.<br />
6. Have a clean user interface for controlling the APS.<br />

<b>Current Progress (As of 1 June 2015):</b><br />
1. Roundtrip currently uses the CareLink stick connected to the phone via USB OTG to transmit/receive data to/from pump.<br />
2. The app can currently retreive pump history, but is limited to the first page. It can separate the history data.<br />
3. The app can set temp basals.<br />
4. Much of the APS logic for making automating corrections is done.  This has been tested on a limited basis in a prototype device done in python on the Raspberry Pi.<br />
5. CGM data can currently be read from a mongo database.<br />

<b>To Do:</b><br />
1. Complete the app's ability to retreive and parse necessary pump history required for automated corrections. Ideally, the setting specificied in the pump itself will be used for insulin sensitivy, BG limits, correction factor, max basal rate, and basal rate profile.<br />
2. Finish the OpenAPS logic.<br />
3. Add a UI.  Include the ability to turn program on/off, suspend APS for x minutes and resume, set target BG, specify DIA (if different setting than one in pump is needed), and specify any other settings required but not retreived from pump.<br />
