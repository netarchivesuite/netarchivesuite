COPY schedules (schedule_id, name, comments, startdate, enddate, maxrepeats, timeunit, numtimeunits, anytime, onminute, onhour, ondayofweek, ondayofmonth, edition) FROM stdin;
1       Once a day              \N      \N      \N      2       1       t       \N      \N      \N      \N      1
2       Once a month            \N      \N      \N      4       1       t       \N      \N      \N      \N      1
3       Once a week             \N      \N      \N      3       1       t       \N      \N      \N      \N      1
4       Once an hour            \N      \N      \N      1       1       t       \N      \N      \N      \N      1
5       Once            \N      \N      1       1       1       t       \N      \N      \N      \N      1

\.

