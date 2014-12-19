db.location.ensureIndex({"c.a.l": "2dsphere"});
db.location.ensureIndex( { "_id" : 1, "lac.n": 1 }, {unique: 1} );

db.check_in.ensureIndex({"lo.l": "2dsphere"});

db.cache_friends.ensureIndex( { "t": 1 }, { expireAfterSeconds: 600 } );
db.cache_friends.ensureIndex( { uid:1, gid:1 } );

db.cache_sessions.ensureIndex( { "t": 1 }, { expireAfterSeconds: 15811200 } );

db.cache_useronline.ensureIndex( { "t": 1 }, { expireAfterSeconds: 600} );

db.cache_writesniffer.ensureIndex( { "t": 1 }, { expireAfterSeconds: 60} )

db.adm_credentials.ensureIndex({un: 1}, {unique: 1})

db.news_like.ensureIndex({ nid:1, uid:1 }, {unique: 1})
db.wish_likes.ensureIndex({ wid:1, uid:1 }, {unique: 1})
db.location_news_likes.ensureIndex({ lnid:1, uid:1 }, {unique: 1})
db.location_photo_likes.ensureIndex({ pid:1, uid:1 }, {unique: 1})
db.photo_like.ensureIndex({ pid:1, uid:1 }, {unique: 1})

db.product.ensureIndex( { rid: 1 } )
db.product.ensureIndex( { rid: 1, pc: 1 } )
db.product.ensureIndex( { rid:1, pc: 1, "n.ru": 1 } )
