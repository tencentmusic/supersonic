import { useState } from "react"
import Explain from "./Explain"
import FilterConditions from "./FilterConditions"

export default function Index() {
  const [editing, setEditing] = useState(false)

  return editing ? (
    <FilterConditions />
  ): (
    <Explain />
  )
}
