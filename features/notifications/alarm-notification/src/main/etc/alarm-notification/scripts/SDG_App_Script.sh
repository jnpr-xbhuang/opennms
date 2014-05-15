echo "SDG alarm notification script started "
logger SDG alarm notification script started
xml=$1
spcialNodeCount=`grep -ce "spaceCluster-VIP" /etc/hosts`
if [ $spcialNodeCount -gt 0 ];then
  spaceClusterIp=`awk '/spaceCluster-VIP/{print $1}' /etc/hosts`
  ipEntryCount=`/etc/init.d/iptables status|grep -ce $spaceClusterIp`
  if [ $ipEntryCount -eq 0 ];then
     iptables -I OUTPUT 1 -d spaceCluster-VIP -p all -j ACCEPT
     logger IP Table entry added
  fi
  curl -k -v -u super:juniper123 -X POST -H "Content-type:application/xml" -d "$xml" "https://spaceCluster-VIP/api/sgd/fault/sdg-fault/sdgalarm"
else
  curl -v -u \$\$\$rest: -X POST -H "Content-type:application/xml" -d "$xml" "http://jmp-cluster:8080/api/sgd/fault/sdg-fault/sdgalarm"
  logger Alarm xml sent $xml
fi
