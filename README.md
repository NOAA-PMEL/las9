# Live Access Serever 9 (repository: las9)

This version of LAS has the following technical features.

1. All configuration and management done using an Admin Console UI provide with LAS. Admin functions include:
      1. Add, remove, temporarily disable a data set
      2. Rearrange the heirarchy of how data sets are displayed in the UI
      3. Automatically check data sets for new time steps on a schedule set in the UI
      4. Incorporate interactive client-side plotting capability
      5. Simple deployment using pre-packaged war file (including F-TDS as its own companion war file).
      6. Dynamic data set configuration where appropriate (including on background thread).
3. Simplified client-server interaction using JSON messages automatically marshalled to and from Java.
4. Nicely styled, modern client UI look-and-feel.
5. Simple upgrade process (save the persistence database and drop in the new war file).
