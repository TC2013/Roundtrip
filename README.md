# Roundtrip

Roundtrip is a limited port of OpenAPS (author Ben West) into an Android application.  

<b>Goals:</b><br />
1. Receive/transmit data from Medtronic Pumps.<br />
2. Receive Dexcom CGM date via bluetooth.<br />
3. Implement the "reference design" artificial pancreas software as designed by Dana Lewis and Scott Leibrand (openaps.org).<br />
4. Upload relevant treatment decisions made by APS to Nightscout.<br />
5. Use Rileylink (https://github.com/ps2/rileylink, author Pete Schwamb) to transmit/receive data from Android via bluetooth to pump.<br />
6. Have a clean user interface for controlling the APS.<br />

<b>Current Progress (As of 16 June 2015):</b><br />
1. Roundtrip currently uses the CareLink stick connected to the phone via USB OTG to transmit/receive data to/from pump.  Order placed for RileyLink!<br />
2.  The APS functions are working.  The app can currently retrieve and parse both Bolous Wizard history and temp basal history needed for the APS logic.<br />
3. The app can retrieve pump setting, calculate IOB/COB and set temp basals if necessary.<br />
4. CGM data can currently be read from a mongo database. The code is there to pull CGM data from xDrip via intents, though no UI selection currently.<br />

<b>To Do:</b><br />
1. Having some occasional challenges with date time stamps not matching up right.  Time is recorded in different formats on pump, dex, and phone, creating some unique problems.<br />
2. Improve UI. The list of improvements needed here is long, so I will just leave it at that.<br />
3. Currently the app is only tested with a 722 pump.  We will likely need patches for other pumps to function correctly.<br />
4. Add the ability to use the RileyLink! <br/>
