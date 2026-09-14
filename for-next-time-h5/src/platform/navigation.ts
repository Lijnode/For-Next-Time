export function goBack() {
  window.history.back()
}

export function replaceWithLogin() {
  window.location.replace(`${import.meta.env.BASE_URL}login`)
}
