function process(event) {
  if (event.getApp_id() === "drop-all") {
    event.drop()
  }

  return [];
};

