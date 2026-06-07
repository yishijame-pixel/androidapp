/// <reference path="../pb_data/types.d.ts" />
/** 允许画家 PATCH 自己的 game_moves（笔画预览增量同步） */
migrate((app) => {
  const collection = app.findCollectionByNameOrId("game_moves")
  if (!collection) return
  collection.updateRule = "@request.auth.id = player"
  return app.save(collection)
}, (app) => {
  const collection = app.findCollectionByNameOrId("game_moves")
  if (!collection) return
  collection.updateRule = null
  return app.save(collection)
})
