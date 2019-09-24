#!/usr/bin/env bash

declare -A unapproved

for source_file in $(find -type f -wholename */main/**/*.scala); do
  class_name=$(basename "${source_file}" ".scala")
  source_lines_count=$(cat "${source_file}" | wc --lines)
  expected_test_location=$(dirname $(echo "${source_file}" | sed "s/main/test/g"))

  for test in "${expected_test_location}/${class_name}*.scala"; do
    if [[ -e "${test}" ]]; then
      test_lines_count=$(cat "${test}" | wc --lines)
      if [[ "${test_lines_count}" -lt "${source_lines_count}" ]]; then
        unapproved[${source_file}]+="Add more tests"
      fi
    else
      unapproved[${source_file}]+="Create tests"
    fi
  done
done

cat << EOF
--------------------------------------------------------------------------------
-----██--██--██--██---████---█████---█████----████---██--██--██████--█████------
-----██--██--██--██--██--██--██--██--██--██--██--██--██--██--██------██--██-----
-----██--██--███-██--██--██--██--██--██--██--██--██--██--██--██████--██--██-----
-----██--██--██-███--██████--█████---█████---██--██---████---██------██--██-----
-----██████--██--██--██--██--██------██------██--██----██----██------██--██-----
------████---██--██--██--██--██------██-------████-----██----██████--█████------
--------------------------------------------------------------------------------
------------------------------------:::::::::-----------------------------------
---------------------------::::::/++soyhys//::/o--------------------------------
--------------------------:y/:/oyhhhhyhhddhhyyss/-------------------------------
-------------------------:/sydddhhhdmdmNNmmdddddy:------------------------------
-------------------------:ydmmmddddmmmmdhhdddddddo:-----------------------------
---------------------::::/hmmmmdhyyssoso++osyhmmmds:----------------------------
----------------:::::::::odmmdhso+////////++ooyhdmdo:---------------------------
--------------::::::::::/hmmhsoo+++//+++++++oooshddh/::-------------------------
----------:::::::::::::/sdmdsooo+++++++++++++oosyddds:::::----------------------
-----:::::::::::::::::/+hdmdsooo++++++++++oosssssdmdh/::::-:--------------------
-:---:::::::::::::::::/sdmmhossyhhhysooosyhhyyyssdmmds:::::::-------------------
---:::::::::::::::::::+hNmmyosydddhhhs+oyhhhmddyosmmmh+::::::::-----------------
:::::::::::::::::::::/smmddsooshyyssoo++ooosyysooohmNds::::::::::---------------
:::::::::::::::::::://ydmmhs++++++++oo++oo+/+++++ohNNdh/:::::::::---------------
::::::::::::::::::://+hmmNhyoo+++++ossooss+++++ooshNmdho::::::::::---:::--------
::::::::::::::::::://odmmmmysooooo++shyyhy+++oosohmNmdhs::::::::::--::::--------
::::::::::::::::::://ohdmmNmdssssoooooooooooossssNNNdhho:::::::::::::::::-------
::::::::::::::::::///+hddmNmNyssssyyhhhhhhyyyssshNNmddh+::::::::::::::::::-:----
:::::::::::::::::::///oddmmmNNsssssssyhhyysssssyNNmmmho/::::::::::::::::::::----
:::::::::::::::::::////ohmmdmNmyssssoooooosssydNNNNmhs///:::::::::::::::::::----
::::::::::::::::::://///odmmdNmyyyysyyyhyyyyyhNNNmmho+////:::::::::::::::::::---
::::::::::::::::::://////+hmdmNyyyyyhhhhhhhysdNNmho+++////:::::::::::::::::::::-
:::::::::::::::::::///////+shmhyyyyyyhhhhyyssdmmdh++++/////:::::::::::::::::::::
::::::::::::::::::///////++ooyyssyyyyyyyyssssyyo++++++/////::::::::::::::::::::-
:::::::::::::::::://////oo+osssssssyyyyysssssssoo++++++/////::::::::::::::::::::
:::::::::::::::://///oyddo++oooooosssyyysssssooooomhs++++///::::::::::::::::::::
::::::::::::::://yddmNNNmy+++oooooo+oosssssoooooohmNNNdysoo/::::::::::::::::::::
:::::::::::://+++mNNNNNNmmyo+oo+++++++++++ooooosdmNNNNNNNNms//::::::::::::::::::
::::::::::/++++//dNNNNNNNNNmhsso+//++++oossyyddmNNNNNNNNNNd+++//::::::::::::::::
:::::::::/++++///dNNNNNNNNNNNNNNNNmmmmNNNNNNNNNNNNNNNNNNNNh////++/::::::::::::::
::::::::/oo++++++mNNNNNNNNNNNNNNNNNNNNNNNNNmNmNNNmNNNNNNNNh/////++/:::::::::::::
::::::::/oo++++++mNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNd++++++++:::::::::::::
:::::://+oo+++++omNNNNNNNNNNNNNNNNNNmNNNNNNNNNNNNNNNmNNNNNh++++++oo/::::::::::::
:::::///oo++++++omNNNNmNNNNNNNNNNNNNNNNNNNNNNNmNmNNmmNNNNNh++++++oo/::::::::::::
:::::///ooo+++oosmNNNNmNmNNNNNNNNNNNNNNNNNNNNNNNmNmmmmNmNNho+++++oo+::::::::::::
::::///+ooo+ooosymNNNNNNNNNNNNNNNNNNNNNNNNNNNmNNmNmNNmmmmNdoo+++++oo::::::::::::
::::///+oooooooshmNNNNNNmNNmmNNNNNNNNNNNNNNNNNmmmmNNNNNNNNdso++++ooo::::::::::::
::::///+oooooooshmNNNNNNNmmmmNmmNNNNNNNNNNNNNmmmmmNNNNNNNNdsoo+++ooo::::::::::::
EOF

for file in "${!unapproved[@]}"; do
  message="${unapproved[$file]}"
  echo "${file}: $message"
done
