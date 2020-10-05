This module mirrors the "ant releasezipball" target in the
old build process.

The resulting artifact has all the needed jars, wars and javadoc, 
but the ordering is different from the old build process as
we want to take advantage of the maven dependency handling to
generate classpaths etc.

/tra 2014-05-21
