#!/bin/bash

mongoimp=mongoimport
mongo=mongo
db=test
datapath=""

$mongoimp -d $db -c adm_user_role --file $datapath""exp_orig_db.adm_user_role.json --drop
$mongoimp -d $db -c country --file $datapath""exp_orig_db.country.json --drop
$mongoimp -d $db -c currency --file $datapath""exp_orig_db.currency.json --drop
$mongoimp -d $db -c eventgroup --file $datapath""exp_orig_db.eventgroup.json --drop
$mongoimp -d $db -c eventtype --file $datapath""exp_orig_db.eventtype.json --drop
$mongoimp -d $db -c location_category --file $datapath""exp_orig_db.location_category.json --drop
$mongoimp -d $db -c maritalstatus --file $datapath""exp_orig_db.maritalstatus.json --drop
$mongoimp -d $db -c moderation_status --file $datapath""exp_orig_db.moderation_status.json --drop
$mongoimp -d $db -c online_status --file $datapath""exp_orig_db.online_status.json --drop
$mongoimp -d $db -c period --file $datapath""exp_orig_db.period.json --drop
$mongoimp -d $db -c present_status --file $datapath""exp_orig_db.present_status.json --drop
$mongoimp -d $db -c product_type --file $datapath""exp_orig_db.product_type.json --drop
$mongoimp -d $db -c region --file $datapath""exp_orig_db.region.json --drop
$mongoimp -d $db -c star_category --file $datapath""exp_orig_db.star_category.json --drop
$mongoimp -d $db -c usersgroup --file $datapath""exp_orig_db.usergroup.json --drop
$mongoimp -d $db -c lifecycle_status --file $datapath""exp_orig_db.lifecycle_status.json --drop

$mongoimp -d $db -c idmapping_dic --file $datapath""exp_map_db.idmapping_dic.csv --drop --type csv -f lid,dn,nid

$mongo localhost/$db info-messages.js
$mongo localhost/$db add_index.js
