/// <reference path="../pb_data/types.d.ts" />
migrate((app) => {
  const collection = app.findCollectionByNameOrId("pbc_2605467279")

  // update collection data
  unmarshal({
    "createRule": "@request.auth.id = sender && (@request.auth.id = member_a || @request.auth.id = member_b)",
    "listRule": "@request.auth.id = member_a || @request.auth.id = member_b",
    "viewRule": "@request.auth.id = member_a || @request.auth.id = member_b"
  }, collection)

  // add field
  collection.fields.addAt(5, new Field({
    "cascadeDelete": false,
    "collectionId": "_pb_users_auth_",
    "help": "",
    "hidden": false,
    "id": "relation_member_a",
    "maxSelect": 1,
    "minSelect": 0,
    "name": "member_a",
    "presentable": false,
    "required": true,
    "system": false,
    "type": "relation"
  }))

  // add field
  collection.fields.addAt(6, new Field({
    "cascadeDelete": false,
    "collectionId": "_pb_users_auth_",
    "help": "",
    "hidden": false,
    "id": "relation_member_b",
    "maxSelect": 1,
    "minSelect": 0,
    "name": "member_b",
    "presentable": false,
    "required": true,
    "system": false,
    "type": "relation"
  }))

  return app.save(collection)
}, (app) => {
  const collection = app.findCollectionByNameOrId("pbc_2605467279")

  // update collection data
  unmarshal({
    "createRule": "@request.auth.id = sender && (@request.auth.id = conversation.member_a || @request.auth.id = conversation.member_b)",
    "listRule": "@request.auth.id != \"\" && (@request.auth.id = sender || @request.auth.id = conversation.member_a || @request.auth.id = conversation.member_b)",
    "viewRule": "@request.auth.id != \"\" && (@request.auth.id = sender || @request.auth.id = conversation.member_a || @request.auth.id = conversation.member_b)"
  }, collection)

  // remove field
  collection.fields.removeById("relation_member_a")

  // remove field
  collection.fields.removeById("relation_member_b")

  return app.save(collection)
})
