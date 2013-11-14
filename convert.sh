RETVAL=0
while [ $RETVAL -eq 0 ]; do
  java -Xmx2g -jar converter.jar /Users/krause/Downloads/Geolife_Trajectories_1.3/Data
  RETVAL=$?
done
echo RETVAL $RETVAL