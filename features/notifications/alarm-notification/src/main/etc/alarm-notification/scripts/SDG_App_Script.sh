echo "SDG alarm notification script started "
logger SDG alarm notification script started
xml=$1
echo "Alarm xml being sent is " $xml
logger Alarm xml sent $xml
curl -v -u super:juniper123 -X POST -H "Content-type:application/xml" -d "$xml" "http://jmp-cluster:8080/api/sgd/fault/sdg-fault/sdgalarm"

