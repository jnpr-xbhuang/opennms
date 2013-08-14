echo "TCA alarm notification script started "
xml=$1
echo "Alarm xml being sent is " $xml
curl -X POST -H "Content-type:application/xml" -d "$xml" "http://jmp-cluster:8080/serviceui/resteasy/tc-alarms"
