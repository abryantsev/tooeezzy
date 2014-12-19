db.idmapping_dic.ensureIndex({dn: 1, lid: 1}, {unique: 1})
db.idmapping_dic.ensureIndex({dn: 1, nid: 1}, {unique: 1})

db.idmapping.ensureIndex({cn: 1, lid: 1}, {unique: 1})
db.idmapping.ensureIndex({cn: 1, nid: 1}, {unique: 1})

db.region.ensureIndex({"cid": 1, "st.sc":1})
db.region.ensureIndex({"cid": 1, "st.l":1})
db.region.ensureIndex({"cid": 1, "st.p":1})
db.region.ensureIndex({"cid": 1, "st.s":1})
db.region.ensureIndex({"cid": 1, "st.u":1})
db.region.ensureIndex({"cid": 1, "st.f":1})

db.period.ensureIndex({"cs": 1})

db.online_status.ensureIndex({"cs": 1})

db.check_in.ensureIndex({"lo.l": "2dsphere"})
db.check_in.ensureIndex({"u.uid": 1})
db.check_in.ensureIndex({"lo.lid": 1})
db.check_in.ensureIndex({"lo.lid": 1})
db.check_in.ensureIndex({"t": 1}, { expireAfterSeconds: 10800})

db.credentials.ensureIndex({un: 1}, {unique:1})
db.adm_credentials.ensureIndex({un: 1}, {unique:1})

db.cache_writesniffer.ensureIndex( { "t": 1 }, { expireAfterSeconds: 60} )

db.cache_sessions.ensureIndex( { "t": 1 }, { expireAfterSeconds: 15811200 } )
db.cache_sessions.ensureIndex( { "uid": 1 } )

db.cache_useronline.ensureIndex( { "t": 1 }, { expireAfterSeconds: 600} )

db.cache_friends.ensureIndex( { "t": 1 }, { expireAfterSeconds: 600 } )
db.cache_friends.ensureIndex( { uid:1, gid:1 } )

db.location.ensureIndex({"c.a.l": "2dsphere"})

db.news_like.ensureIndex({ nid:1, uid:1 }, {unique: 1})
db.wish_likes.ensureIndex({ wid:1, uid:1 }, {unique: 1})
db.location_news_likes.ensureIndex({ lnid:1, uid:1 }, {unique: 1})
db.location_photo_like.ensureIndex({ pid:1, uid:1 }, {unique: 1})
db.photo_like.ensureIndex({ pid:1, uid:1 }, {unique: 1})