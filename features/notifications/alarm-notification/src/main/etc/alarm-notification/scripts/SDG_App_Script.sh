echo "SDG alarm notification script started "
logger SDG alarm notification script started
xml=$1
spcialNodeCount=`grep -ce "spaceCluster-VIP" /etc/hosts`
if [ $spcialNodeCount -gt 0 ];then
  curl -k -v -u super:juniper123 -X POST -H "Content-type:application/xml" -d "$xml" "https://spaceCluster-VIP/api/sgd/fault/sdg-fault/sdgalarm"
  logger Alarm xml sent $xml
else
  curl -v -u \$\$\$rest: -X POST -H "Content-type:application/xml" -d "$xml" "http://jmp-cluster:8080/api/sgd/fault/sdg-fault/sdgalarm"
  logger Alarm xml sent $xml
fi
