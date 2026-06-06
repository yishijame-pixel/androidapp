/// <reference path="../pb_data/types.d.ts" />
migrate((app) => {
  const collection = app.findCollectionByNameOrId("pbc_2955039625")

  // update collection data
  unmarshal({
    "updateRule": "@request.auth.id = host || @request.auth.id = guest || (status = 'waiting' && invite_mode = 'open')"
  }, collection)

  return app.save(collection)
}, (app) => {
  const collection = app.findCollectionByNameOrId("pbc_2955039625")

  // update collection data
  unmarshal({
    "updateRule": "@request.auth.id = host || @request.auth.id = guest"
  }, collection)

  return app.save(collection)
})
