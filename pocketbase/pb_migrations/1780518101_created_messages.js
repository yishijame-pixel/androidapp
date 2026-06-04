/// <reference path="../pb_data/types.d.ts" />
migrate((app) => {
  const collection = new Collection({
    "createRule": "@request.auth.id = sender && (@request.auth.id = conversation.member_a || @request.auth.id = conversation.member_b)",
    "deleteRule": "",
    "fields": [
      {
        "autogeneratePattern": "[a-z0-9]{15}",
        "help": "",
        "hidden": false,
        "id": "text3208210256",
        "max": 15,
        "min": 15,
        "name": "id",
        "pattern": "^[a-z0-9]+$",
        "presentable": false,
        "primaryKey": true,
        "required": true,
        "system": true,
        "type": "text"
      },
      {
        "cascadeDelete": true,
        "collectionId": "pbc_728114816",
        "help": "",
        "hidden": false,
        "id": "relation_conversation",
        "maxSelect": 1,
        "minSelect": 0,
        "name": "conversation",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "relation"
      },
      {
        "cascadeDelete": false,
        "collectionId": "_pb_users_auth_",
        "help": "",
        "hidden": false,
        "id": "relation_sender",
        "maxSelect": 1,
        "minSelect": 0,
        "name": "sender",
        "presentable": false,
        "required": true,
        "system": false,
        "type": "relation"
      },
      {
        "autogeneratePattern": "",
        "help": "",
        "hidden": false,
        "id": "text_body",
        "max": 2000,
        "min": 1,
        "name": "body",
        "pattern": "",
        "presentable": true,
        "primaryKey": false,
        "required": true,
        "system": false,
        "type": "text"
      },
      {
        "hidden": false,
        "id": "autodate2990389176",
        "name": "created",
        "onCreate": true,
        "onUpdate": false,
        "presentable": false,
        "system": false,
        "type": "autodate"
      }
    ],
    "id": "pbc_2605467279",
    "indexes": [
      "CREATE INDEX `idx_msg_conversation_created` ON `messages` (`conversation`, `created`)"
    ],
    "listRule": "@request.auth.id != \"\" && (@request.auth.id = sender || @request.auth.id = conversation.member_a || @request.auth.id = conversation.member_b)",
    "name": "messages",
    "system": false,
    "type": "base",
    "updateRule": "",
    "viewRule": "@request.auth.id != \"\" && (@request.auth.id = sender || @request.auth.id = conversation.member_a || @request.auth.id = conversation.member_b)"
  });

  return app.save(collection);
}, (app) => {
  const collection = app.findCollectionByNameOrId("pbc_2605467279");

  return app.delete(collection);
})
