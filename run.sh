nohup java -Xms4g -Xmx4g -server -XX:SurvivorRatio=6 -XX:NewRatio=1 -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+CMSScavengeBeforeRemark -XX:CMSMaxAbortablePrecleanTime=5000 -XX:CMSInitiatingOccupancyFraction=70 -XX:+CMSClassUnloadingEnabled -XX:+DisableExplicitGC -XX:+CrashOnOutOfMemoryError -jar meepo-data-transform.jar --spring.config.location=/home/xxx/application.properties  > /dev/null 2>&1 &

nohup java -Xms8g -Xmx8g -server -XX:SurvivorRatio=6 -XX:NewSize=6g -XX:MaxNewSize=6g -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+CMSScavengeBeforeRemark -XX:CMSMaxAbortablePrecleanTime=5000 -XX:CMSInitiatingOccupancyFraction=70 -XX:+CMSClassUnloadingEnabled -XX:+DisableExplicitGC -XX:+CrashOnOutOfMemoryError -jar meepo-data-transform.jar --spring.config.location=/home/xxx/application.properties > /dev/null 2>&1 &
