db.getCollectionNames().forEach(function (name) {
	if (name.indexOf("system.") == -1) db.getCollection(name).remove()
});

db.customers.save({
    "firstName":"Joe",
    "lastName":"Black",
    "email":"joe@tooe.com",
    "id":UUID("00000000000000000000000000000000"),
    "addresses":[
        {"line1":"Magdalen Centre", "line2":"Robert Robinson Avenue", "line3":"Oxford"},
        {"line1":"Houldsworth Mill", "line2":"Houldsworth Street", "line3":"Reddish"}
    ]
});