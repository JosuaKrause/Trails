RETVAL=1
while [ $RETVAL -ne 0 ]; do
  java -Xmx2g -jar converter.jar Geolife_Trajectories_1.3/Data
  RETVAL=$?
  sleep 1
done
echo RETVAL $RETVAL