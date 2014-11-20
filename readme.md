Trails
======

See [the handout] (handout.pdf) for a short description of the project.
There is also a video on [youtube] (https://www.youtube.com/watch?v=Hvts19JL1k0)
showing the tool in action using New York CitiBike data and comparing different times of work and weekend days.

In order to set up the project you need a `MySQL` database
accessible via `localhost` on port `8889`. The user-name and
password must be `root`. I haven't come to making it configurable --
you can send a pull request though :)

First run `trails.io.DCLoader` and after filling the database you
can start the application via `trails.Main`.
