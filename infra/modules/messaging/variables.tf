variable "name_prefix" {
  type = string
}

variable "consumers" {
  type = list(string)
}

variable "max_receive_count" {
  type    = number
  default = 3
}
