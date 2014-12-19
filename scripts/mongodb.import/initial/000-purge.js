db.adminCommand("listDatabases").databases.forEach( function (d) {
  if (d.name == "test")
	db.getSiblingDB(d.name).dropDatabase();
});