# Nota: los nombres de atributos (cdaId, companyCode, userId) están en camelCase
# porque así los mapea el DynamoDB Enhanced Client desde las @DynamoDbBean.

resource "aws_dynamodb_table" "cdas" {
  name         = "trust_cdas"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "cdaId"

  attribute {
    name = "cdaId"
    type = "S"
  }

  attribute {
    name = "companyCode"
    type = "S"
  }

  global_secondary_index {
    name            = "company_code-index"
    hash_key        = "companyCode"
    projection_type = "ALL"
  }

  tags = local.tags
}

resource "aws_dynamodb_table" "users" {
  name         = "trust_users"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  tags = local.tags
}

resource "aws_dynamodb_table" "user_cda" {
  name         = "trust_user_cda"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "cdaId"
  range_key    = "userId"

  attribute {
    name = "cdaId"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  tags = local.tags
}
