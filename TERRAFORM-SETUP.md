# Terraform部署配置指南

## 前置条件

在使用Terraform部署EcoGo之前，您需要：

### 1. AWS账户设置

1. 创建AWS账户
2. 安装并配置AWS CLI：
   ```bash
   aws configure
   ```

### 2. 创建S3后端（可选，用于团队协作）

```bash
# 创建Terraform状态存储桶
aws s3api create-bucket \
  --bucket ecogo-terraform-state \
  --region us-east-1

# 启用版本控制
aws s3api put-bucket-versioning \
  --bucket ecogo-terraform-state \
  --versioning-configuration Status=Enabled

# 创建DynamoDB表用于状态锁定
aws dynamodb create-table \
  --table-name ecogo-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1
```

然后在 `terraform/main.tf` 中取消注释S3后端配置：

```terraform
backend "s3" {
  bucket         = "ecogo-terraform-state"
  key            = "ecogo/terraform.tfstate"
  region         = "us-east-1"
  encrypt        = true
  dynamodb_table = "ecogo-terraform-locks"
}
```

### 3. 创建ECR仓库

```bash
# 创建ECR仓库
aws ecr create-repository \
  --repository-name ecogo \
  --region us-east-1

# 获取仓库URI
aws ecr describe-repositories \
  --repository-names ecogo \
  --region us-east-1 \
  --query 'repositories[0].repositoryUri' \
  --output text
```

将输出的URI更新到 `terraform/terraform.tfvars` 中的 `ecr_repository_url`。

### 4. 构建并推送Docker镜像

```bash
# 登录到ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

# 构建镜像
docker build -t ecogo:latest .

# 标记镜像
docker tag ecogo:latest YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/ecogo:latest

# 推送镜像
docker push YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/ecogo:latest
```

## 使用Terraform部署

### 1. 初始化Terraform

```bash
cd terraform
terraform init
```

### 2. 配置变量

编辑 `terraform.tfvars` 文件，更新以下配置：

```hcl
ecr_repository_url = "YOUR_ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/ecogo"
mongodb_uri        = "mongodb://your-mongodb-atlas-uri"  # 使用MongoDB Atlas或自建
```

### 3. 预览部署计划

```bash
terraform plan
```

### 4. 执行部署

```bash
terraform apply
```

### 5. 获取输出信息

```bash
terraform output
```

这将显示ALB的DNS名称，您可以通过该地址访问应用程序。

## GitHub Actions配置

在GitHub仓库中配置以下Secrets：

1. `AWS_ACCESS_KEY_ID` - AWS访问密钥ID
2. `AWS_SECRET_ACCESS_KEY` - AWS秘密访问密钥

配置方法：
1. 进入GitHub仓库
2. Settings → Secrets and variables → Actions
3. 点击 "New repository secret"
4. 添加上述secrets

## 故障排除

### Terraform后端初始化失败

如果您不需要远程状态存储，可以使用本地后端（已默认配置）。

### ECR认证失败

确保您的AWS凭证有ECR的访问权限：

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    }
  ]
}
```

### ECS任务无法启动

检查：
1. ECR镜像是否成功推送
2. MongoDB连接字符串是否正确
3. 安全组配置是否允许流量
4. CloudWatch日志查看详细错误信息

## 清理资源

```bash
cd terraform
terraform destroy
```

## 成本估算

基本配置的预估月度成本（us-east-1）：
- ECS Fargate (256 CPU, 512 MB): ~$15/月
- ALB: ~$16/月
- NAT Gateway: 如果使用则~$32/月
- CloudWatch Logs: ~$0.50/月

**总计：约$31-63/月**（取决于配置）

## 参考文档

- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [AWS ECR Documentation](https://docs.aws.amazon.com/ecr/)
