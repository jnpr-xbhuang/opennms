echo "CBU alarm notification script started " >> start.log
xml=$1
echo "CBU Alarm xml being sent is " $xml >> start.log
curl -v -u super:juniper123 -X POST -H "Content-type:application/xml" -d "$xml" "https://JMP-CLUSTER/api/juniper/packet-optical/service-alarm-manager/alarm" --insecure
echo " POD Rest Web Service is Invoked " >> start.log

